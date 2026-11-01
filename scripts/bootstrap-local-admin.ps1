$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Get-Content -LiteralPath (Join-Path $projectRoot '.env') | ForEach-Object {
    if ($_ -match '^\s*([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
        [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], 'Process')
    }
}
$email = Read-Host 'First administrator email'
$displayName = Read-Host 'Display name'
$securePassword = Read-Host 'Temporary password (12 or more characters)' -AsSecureString
$pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
try {
    $plainPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    [Environment]::SetEnvironmentVariable('ERP_INITIAL_PASSWORD', $plainPassword, 'Process')
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    $plainPassword = $null
}
try {
    $repository = Join-Path $env:USERPROFILE '.m2\repository'
    $postgres = Get-ChildItem (Join-Path $repository 'org\postgresql\postgresql\*\postgresql-*.jar') | Sort-Object FullName -Descending | Select-Object -First 1
    $crypto = Get-ChildItem (Join-Path $repository 'org\springframework\security\spring-security-crypto\*\spring-security-crypto-*.jar') | Sort-Object FullName -Descending | Select-Object -First 1
    $logging = Get-ChildItem (Join-Path $repository 'commons-logging\commons-logging\*\commons-logging-*.jar') | Sort-Object FullName -Descending | Select-Object -First 1
    if (!$postgres -or !$crypto -or !$logging) { throw 'Run the backend Maven build first to download the required libraries.' }
    $classPath = "$($postgres.FullName);$($crypto.FullName);$($logging.FullName)"
    & java -cp $classPath (Join-Path $PSScriptRoot 'BootstrapLocalAdmin.java') $email $displayName
    if ($LASTEXITCODE -ne 0) { throw 'Administrator bootstrap failed.' }
} finally {
    Remove-Item Env:ERP_INITIAL_PASSWORD -ErrorAction SilentlyContinue
}
