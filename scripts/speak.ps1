param([string]$text)

if (-not $text -or $text.Trim() -eq "") {
    try {
        $text = [Console]::In.ReadToEnd()
    } catch {}
}

if (-not $text -or $text.Trim() -eq "") { 
    exit 0 
}

try {
    Add-Type -AssemblyName System.Speech -ErrorAction Stop
    $speak = New-Object System.Speech.Synthesis.SpeechSynthesizer
    $speak.Rate = 1
    $speak.Speak($text.Trim())
} catch {
    # Fallback to SAPI COM object
    try {
        $voice = New-Object -ComObject SAPI.SpVoice
        $voice.Rate = 1
        $voice.Speak($text.Trim())
    } catch {
        exit 1
    }
}
