param(
    [string]$BaseUrl = 'http://localhost:8080'
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
if ($results.Count -ne 9 -or ($results | Where-Object { -not $_.Passed })) {
    throw 'Security verification failed: compare the expected and actual responses above.'
}
Write-Output 'All nine real HTTP checks passed.'
