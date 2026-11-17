param(
  [switch]$SkipSign,
  [string]$OutName = '早八.apk',
  [int]$VersionCode = 1,
  [string]$VersionName = '1.0'
)

$ErrorActionPreference = 'Stop'
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $here

$SDK   = 'C:\Program Files (x86)\Android\android-sdk'
$JDK   = 'C:\Program Files\Android\openjdk\jdk-21.0.8'
$BT    = Join-Path $SDK 'build-tools\36.0.0'
$AJ    = Join-Path $SDK 'platforms\android-35\android.jar'
$env:JAVA_HOME = $JDK
$env:PATH = "$JDK\bin;$BT;$env:PATH"

foreach ($tool in @("$BT\aapt2.exe", "$BT\d8.bat", "$BT\zipalign.exe", "$BT\apksigner.bat", "$JDK\bin\javac.exe", $AJ)) {
    if (-not (Test-Path $tool)) { throw "缺工具: $tool" }
}

$out = Join-Path $here 'build'
if (Test-Path $out) { Remove-Item $out -Recurse -Force }
New-Item -ItemType Directory -Force $out, "$out\classes", "$out\dex", "$out\gen" | Out-Null

Write-Host '[1/6] aapt2 编译资源'
$resZip = "$out\res.zip"
if (Test-Path 'res') {
    & "$BT\aapt2.exe" compile --dir res -o $resZip
    if ($LASTEXITCODE -ne 0) { throw 'aapt2 compile 失败' }
}

Write-Host '[2/6] aapt2 链接清单'
$base = "$out\base.apk"
$linkArgs = @('link', '-o', $base, '-I', $AJ, '--manifest', 'AndroidManifest.xml',
              '--java', "$out\gen", '--min-sdk-version', '24', '--target-sdk-version', '33',
              '--version-code', "$VersionCode", '--version-name', $VersionName, '--auto-add-overlay')
if (Test-Path 'assets') { $linkArgs += @('-A', 'assets') }
if (Test-Path $resZip) { $linkArgs += $resZip }
# aapt2 的 --version-code 仅在清单缺省时生效，因此按参数回写清单，保证唯一来源
$mf = Join-Path $here 'AndroidManifest.xml'
$mt = [IO.File]::ReadAllText($mf)
$mt2 = [regex]::Replace($mt, 'android:versionCode="\d+"', "android:versionCode=`"$VersionCode`"")
$mt2 = [regex]::Replace($mt2, 'android:versionName="[^"]*"', "android:versionName=`"$VersionName`"")
if ($mt2 -ne $mt) {
    [IO.File]::WriteAllText($mf, $mt2)
    Write-Host "  清单版本已回写: $VersionCode / $VersionName"
}

& "$BT\aapt2.exe" @linkArgs
if ($LASTEXITCODE -ne 0) { throw 'aapt2 link 失败' }

Write-Host '[3/6] javac 编译 Java'
$srcFiles = @()
if (Test-Path 'src') { $srcFiles += (Get-ChildItem src -Recurse -Filter *.java | ForEach-Object { $_.FullName -replace '\\', '/' }) }
$genFiles = @(Get-ChildItem "$out\gen" -Recurse -Filter *.java -ErrorAction SilentlyContinue | ForEach-Object { $_.FullName -replace '\\', '/' })
$srcFiles += $genFiles
if ($srcFiles.Count -eq 0) { throw '没有 Java 源文件' }
& "$JDK\bin\javac.exe" -encoding UTF-8 --release 8 -nowarn -cp $AJ -d "$out\classes" $srcFiles
if ($LASTEXITCODE -ne 0) { throw 'javac 失败' }

Write-Host '[4/6] d8 生成 dex'
$classFiles = Get-ChildItem "$out\classes" -Recurse -Filter *.class | ForEach-Object { $_.FullName }
$d8Args = @('--lib', $AJ, '--min-api', '24', '--output', "$out\dex") + $classFiles
& "$BT\d8.bat" @d8Args
if ($LASTEXITCODE -ne 0) { throw 'd8 失败' }

Write-Host '[5/6] 打包 APK'
Add-Type -AssemblyName System.IO.Compression, System.IO.Compression.FileSystem
$unsigned = "$out\unsigned.apk"
$src = [System.IO.Compression.ZipFile]::OpenRead($base)
$dst = [System.IO.Compression.ZipFile]::Open($unsigned, 'Create')
foreach ($e in $src.Entries) {
    $entryName = $e.FullName.Replace('\', '/')
    $ne = $dst.CreateEntry($entryName, [System.IO.Compression.CompressionLevel]::NoCompression)
    $inS = $e.Open(); $outS = $ne.Open(); $inS.CopyTo($outS); $inS.Close(); $outS.Close()
}
$src.Dispose()
$dex = "$out\dex\classes.dex"
if (-not (Test-Path $dex)) { throw 'classes.dex 没生成' }
$ne = $dst.CreateEntry('classes.dex', [System.IO.Compression.CompressionLevel]::NoCompression)
$inS = [IO.File]::OpenRead($dex); $outS = $ne.Open(); $inS.CopyTo($outS); $inS.Close(); $outS.Close()
$dst.Dispose()

Write-Host '[6/6] zipalign + 签名'
$aligned = "$out\aligned.apk"
& "$BT\zipalign.exe" -f 4 $unsigned $aligned
if ($LASTEXITCODE -ne 0) { throw 'zipalign 失败' }

if (-not $SkipSign) {
    $ksPass = $env:ZAOBA_KS_PASS
    if (-not $ksPass -and (Test-Path "$here\signing.local.ps1")) {
        . "$here\signing.local.ps1"
        $ksPass = $ZaobaKsPass
    }
    if (-not $ksPass) {
        throw '缺少签名口令：设置环境变量 ZAOBA_KS_PASS，或在 android\signing.local.ps1 中定义 $ZaobaKsPass'
    }
    $ks = "$here\zaoba.jks"
    if (-not (Test-Path $ks)) {
        & "$JDK\bin\keytool.exe" -genkeypair -v -keystore $ks -alias zaoba -keyalg RSA -keysize 2048 `
            -validity 10000 -storepass $ksPass -keypass $ksPass `
            -dname 'CN=Zaoba, OU=Mobile, O=Zaoba, L=Chengdu, ST=Sichuan, C=CN' 2>&1 | Out-Null
        if ($LASTEXITCODE -ne 0) { throw 'keytool 失败' }
    }
    $final = Join-Path $here $OutName
    if (Test-Path $final) { Remove-Item $final -Force }
    & "$BT\apksigner.bat" sign --ks $ks --ks-pass pass:$ksPass --key-pass pass:$ksPass --out $final $aligned
    if ($LASTEXITCODE -ne 0) { throw 'apksigner 失败' }
    & "$BT\apksigner.bat" verify --print-certs $final | Select-Object -First 6
    $size = [math]::Round((Get-Item $final).Length / 1KB, 1)
    Write-Host ''
    Write-Host "OK -> $final ($size KB)"
} else {
    Write-Host "OK (未签名) -> $aligned"
}
