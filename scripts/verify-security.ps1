param(
    [string]$BaseUrl = 'http://localhost:8080',
    [string]$ReportPath
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http

# Public classroom credentials from the assignment, not real account passwords.
$callers = @(
    @{ Name = 'Anonymous'; Credentials = $null; Expected = @(200, 401, 401) },
    @{ Name = 'USER'; Credentials = 'student:student123'; Expected = @(200, 200, 403) },
    @{ Name = 'ADMIN'; Credentials = 'admin:admin123'; Expected = @(200, 200, 200) }
)
$routes = @(
    @{ Path = '/api/public/ping'; Body = 'Public endpoint is reachable' },
    @{ Path = '/api/hello'; Body = 'Hello, authenticated user' },
    @{ Path = '/api/admin/report'; Body = 'Confidential admin report' }
)

$handler = [System.Net.Http.HttpClientHandler]::new()
$handler.AllowAutoRedirect = $false
$handler.UseCookies = $false
$client = [System.Net.Http.HttpClient]::new($handler)
$client.Timeout = [TimeSpan]::FromSeconds(10)
$results = @()
$responses = @()
$startedAt = [DateTimeOffset]::Now.ToString('o')

try {
    foreach ($caller in $callers) {
        for ($index = 0; $index -lt $routes.Count; $index++) {
            $route = $routes[$index]
            $request = [System.Net.Http.HttpRequestMessage]::new(
                [System.Net.Http.HttpMethod]::Get,
                $BaseUrl.TrimEnd('/') + $route.Path
            )
            $request.Headers.Accept.ParseAdd('text/plain')
            if ($caller.Credentials) {
                $encoded = [Convert]::ToBase64String(
                    [System.Text.Encoding]::UTF8.GetBytes($caller.Credentials)
                )
                $request.Headers.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::new(
                    'Basic', $encoded
                )
            }
            try {
                $response = $client.SendAsync($request).GetAwaiter().GetResult()
                try {
                    $actual = [int]$response.StatusCode
                    $body = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
                    $expected = $caller.Expected[$index]
                    $passed = $actual -eq $expected
                    if ($expected -eq 200) {
                        $passed = $passed -and $body -eq $route.Body
                    }
                    $results += [pscustomobject]@{
                        Caller = $caller.Name
                        Path = $route.Path
                        Expected = $expected
                        Actual = $actual
                        Passed = $passed
                    }
                    $responses += [pscustomobject]@{
                        Caller = $caller.Name
                        Method = 'GET'
                        Path = $route.Path
                        Expected = $expected
                        Actual = $actual
                        Passed = $passed
                        Body = $body
                        WwwAuthenticate = @($response.Headers.WwwAuthenticate | ForEach-Object { $_.ToString() })
                    }
                } finally {
                    $response.Dispose()
                }
            } finally {
                $request.Dispose()
            }
        }
    }
} finally {
    $client.Dispose()
}

$results | Format-Table -AutoSize
if ($ReportPath) {
    $report = [ordered]@{
        StartedAt = $startedAt
        BaseUrl = $BaseUrl.TrimEnd('/')
        Verification = 'Real HTTP requests; no mocks'
        Passed = $results.Count -eq 9 -and @($results | Where-Object { -not $_.Passed }).Count -eq 0
        Results = $responses
    }
    $report | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $ReportPath -Encoding utf8
}
if ($results.Count -ne 9 -or ($results | Where-Object { -not $_.Passed })) {
    throw 'Security verification failed: compare the expected and actual responses above.'
}
Write-Output 'All nine real HTTP checks passed.'
