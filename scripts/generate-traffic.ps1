param(
    [string]$BaseUrl = 'http://localhost:8080',
    [ValidateRange(2, 1000)]
    [int]$Requests = 60
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http
$handler = [System.Net.Http.HttpClientHandler]::new()
$handler.UseCookies = $false
$client = [System.Net.Http.HttpClient]::new($handler)
$client.Timeout = [TimeSpan]::FromSeconds(10)
# Published classroom account from the assignment.
$basicAuth = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes('student:student123'))

try {
    for ($index = 0; $index -lt $Requests; $index++) {
        $authenticated = $index % 2 -eq 1
        $path = if ($authenticated) { '/api/hello' } else { '/api/public/ping' }
        $expectedBody = if ($authenticated) { 'Hello, authenticated user' } else { 'Public endpoint is reachable' }
        $request = [System.Net.Http.HttpRequestMessage]::new(
            [System.Net.Http.HttpMethod]::Get, $BaseUrl.TrimEnd('/') + $path
        )
        if ($authenticated) {
            $request.Headers.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::new('Basic', $basicAuth)
        }
        try {
            $response = $client.SendAsync($request).GetAwaiter().GetResult()
            try {
                $response.EnsureSuccessStatusCode() | Out-Null
                $body = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
                if ($body -ne $expectedBody) { throw "Unexpected response from $path" }
            } finally {
                $response.Dispose()
            }
        } finally {
            $request.Dispose()
        }
        if ($index -lt $Requests - 1) { Start-Sleep -Milliseconds 200 }
    }
} finally {
    $client.Dispose()
}

Write-Output "$Requests successful requests sent across /api/public/ping and /api/hello."
Write-Output 'Refresh Grafana with Last 5 minutes selected to see the traffic.'
