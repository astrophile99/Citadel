<#
  Creates the upload key Citadel's release builds are signed with, and writes
  keystore.properties so Gradle can find it. Run once, from the project root:

      powershell -ExecutionPolicy Bypass -File scripts\create-upload-key.ps1

  Both files it creates are gitignored. Keep a copy of the keystore and its password
  somewhere safe outside this folder (a password manager is ideal). With Play App Signing,
  a lost upload key can be reset through Play Console support — but it is far easier to
  simply not lose it.
#>

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$keystore = Join-Path $root "citadel-upload.jks"
$properties = Join-Path $root "keystore.properties"

if (Test-Path $keystore) {
    Write-Host "citadel-upload.jks already exists. Refusing to overwrite an existing key." -ForegroundColor Yellow
    exit 1
}

# keytool ships with Android Studio's bundled JDK; fall back to whatever is on PATH.
$keytool = @(
    "$env:ProgramFiles\Android\Android Studio\jbr\bin\keytool.exe",
    "$env:LOCALAPPDATA\Programs\Android Studio\jbr\bin\keytool.exe"
) | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $keytool) {
    $found = Get-Command keytool -ErrorAction SilentlyContinue
    if ($found) { $keytool = $found.Source }
}
if (-not $keytool) {
    Write-Host "keytool was not found. Install Android Studio, or add a JDK's bin folder to PATH." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Choose a password for the Citadel upload key (at least 6 characters)."
$secure = Read-Host "Password" -AsSecureString
$confirm = Read-Host "Confirm password" -AsSecureString
$plain = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure))
$plainConfirm = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($confirm))
if ($plain -ne $plainConfirm) { Write-Host "The passwords did not match." -ForegroundColor Red; exit 1 }
if ($plain.Length -lt 6) { Write-Host "Use at least 6 characters." -ForegroundColor Red; exit 1 }

$name = Read-Host "Your name, as it should appear on the certificate"
if ([string]::IsNullOrWhiteSpace($name)) { $name = "Citadel" }

& $keytool -genkeypair -v `
    -keystore $keystore `
    -alias citadel `
    -keyalg RSA -keysize 4096 -validity 10000 `
    -storepass $plain -keypass $plain `
    -dname "CN=$name, O=Citadel"
if ($LASTEXITCODE -ne 0) { Write-Host "keytool failed." -ForegroundColor Red; exit 1 }

@"
storeFile=citadel-upload.jks
storePassword=$plain
keyAlias=citadel
keyPassword=$plain
"@ | Set-Content -Path $properties -Encoding ascii

Write-Host ""
Write-Host "Created citadel-upload.jks and keystore.properties (both gitignored)." -ForegroundColor Green
Write-Host "Back up citadel-upload.jks and the password now. Then build the bundle with:"
Write-Host "    .\gradlew :app:bundleRelease"
