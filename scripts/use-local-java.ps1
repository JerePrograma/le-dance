$jdkPath = Join-Path $env:USERPROFILE ".jdks\corretto-21.0.7"
$javaExe = Join-Path $jdkPath "bin\java.exe"
$javacExe = Join-Path $jdkPath "bin\javac.exe"

if (-not (Test-Path -LiteralPath $javaExe)) {
    throw "No se encontró Java en: $javaExe"
}

if (-not (Test-Path -LiteralPath $javacExe)) {
    throw "No se encontró javac en: $javacExe"
}

$env:JAVA_HOME = $jdkPath
$jdkBin = Join-Path $jdkPath "bin"

$pathEntries = $env:Path -split ";" |
    Where-Object {
        $_ -and
        $_ -ne $jdkBin -and
        $_ -notmatch '\\Java\\.*\\bin\\?$' -and
        $_ -notmatch '\\\.jdks\\.*\\bin\\?$'
    }

$env:Path = (@($jdkBin) + $pathEntries) -join ";"

Write-Host "JAVA_HOME configurado para esta terminal:"
Write-Host "  $env:JAVA_HOME"

& $javaExe -version
