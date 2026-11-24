param(
    [string]$BaseUrl = 'http://localhost:8080',
    [string]$PrometheusUrl = 'http://localhost:9090',
    [string]$GrafanaUrl = 'http://localhost:3000',
    [switch]$SkipContainers
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http
$client = [System.Net.Http.HttpClient]::new()
$client.Timeout = [TimeSpan]::FromSeconds(10)

function Get-ResponseText([string]$Url) {
    $response = $client.GetAsync($Url).GetAwaiter().GetResult()
    try {
        $response.EnsureSuccessStatusCode() | Out-Null
        return $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
    } finally {
        $response.Dispose()
    }
}

try {
    $health = Get-ResponseText ($BaseUrl.TrimEnd('/') + '/actuator/health') | ConvertFrom-Json
    if ($health.status -ne 'UP') {
        throw 'Application health is not UP.'
    }
    Write-Output 'Application health: UP (anonymous access).'

    # Generate samples before checking the request counters and histogram.
    for ($requestNumber = 0; $requestNumber -lt 10; $requestNumber++) {
        $body = Get-ResponseText ($BaseUrl.TrimEnd('/') + '/api/public/ping')
        if ($body -ne 'Public endpoint is reachable') {
            throw 'Unexpected public endpoint response.'
        }
    }

    $metrics = Get-ResponseText ($BaseUrl.TrimEnd('/') + '/actuator/prometheus')
    foreach ($metricName in @(
        'jvm_memory_used_bytes',
        'process_cpu_usage',
        'http_server_requests_seconds_count',
        'http_server_requests_seconds_bucket',
        'jvm_threads_live_threads'
    )) {
        if ($metrics -notmatch ('(?m)^' + [regex]::Escape($metricName) + '(?:\{|\s)')) {
            throw "Missing metric: $metricName"
        }
        Write-Output "Metric available: $metricName"
    }

    if (-not $SkipContainers) {
        $query = [Uri]::EscapeDataString('up{job="autoloc-api"}')
        $targetUp = $false
        for ($attempt = 0; $attempt -lt 6; $attempt++) {
            $result = Get-ResponseText ($PrometheusUrl.TrimEnd('/') + '/api/v1/query?query=' + $query) | ConvertFrom-Json
            $targetUp = $result.status -eq 'success' -and @(
                $result.data.result | Where-Object { $_.value[1] -eq '1' }
            ).Count -gt 0
            if ($targetUp) { break }
            if ($attempt -lt 5) { Start-Sleep -Seconds 3 }
        }
        if (-not $targetUp) {
            throw 'Prometheus target autoloc-api is not UP. Check /targets and host.docker.internal:8080.'
        }
        Write-Output 'Prometheus target autoloc-api: UP.'

        $grafana = Get-ResponseText ($GrafanaUrl.TrimEnd('/') + '/api/health') | ConvertFrom-Json
        if ($grafana.database -ne 'ok') {
            throw 'Grafana database health check failed.'
        }
        Write-Output 'Grafana health: OK.'
    }
} finally {
    $client.Dispose()
}

Write-Output 'Monitoring checks passed.'
