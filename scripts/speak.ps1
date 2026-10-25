param([string]$text)
if (-not $text) { exit 0 }
try {
    Add-Type -AssemblyName System.Speech -ErrorAction Stop
    $speak = New-Object System.Speech.Synthesis.SpeechSynthesizer
    $speak.Rate = 1
    $speak.Speak($text)
} catch {
    # Fallback to SAPI COM
    $voice = New-Object -ComObject SAPI.SpVoice
    $voice.Rate = 1
    $voice.Speak($text)
}
