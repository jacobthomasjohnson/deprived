$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$inputRoot = Join-Path $root 'custom-sound-input'
$outputRoot = Join-Path $root 'src\main\resources\assets\deprived\sounds'

function Convert-Sound {
    param([string]$SourceRelative, [string]$Family, [int]$Index, [int]$Loudness = -21)
    $source = Join-Path $inputRoot $SourceRelative
    $folder = Join-Path $outputRoot $Family
    $target = Join-Path $folder "$Index.ogg"
    if (-not (Test-Path -LiteralPath $source)) { throw "Missing input: $source" }
    New-Item -ItemType Directory -Force -Path $folder | Out-Null
    $filter = "aformat=channel_layouts=mono,highpass=f=28,lowpass=f=16000,loudnorm=I=${Loudness}:LRA=12:TP=-2"
    & ffmpeg -y -loglevel error -i $source -map '0:a:0' -vn -af $filter -ar 48000 -c:a libvorbis -q:a 6 $target
    if ($LASTEXITCODE -ne 0) { throw "ffmpeg failed for $source" }
    Write-Host "$Family/$Index <- $SourceRelative"
}

# Long, realistic movement sequences. These are played sparsely, not once per step.
Convert-Sound 'footsteps\footsteps_carpet_01.wav' 'haunted_footstep' 1 -21
Convert-Sound 'footsteps\footsteps_grass_01.wav' 'haunted_footstep' 2 -21
Convert-Sound 'footsteps\footsteps_gravel_01.wav' 'haunted_footstep' 3 -21
Convert-Sound 'footsteps\footsteps_running_grass_01.wav' 'haunted_footstep' 4 -22
Convert-Sound 'footsteps\footsteps_wood_01.wav' 'haunted_footstep' 5 -21

Convert-Sound 'knocking\knocking_door_hollow_01.wav' 'knock' 1 -19
Convert-Sound 'knocking\knocking_door_loudly_01.wav' 'knock' 2 -19
Convert-Sound 'scratching\scraching_soft_wooden.wav' 'scratch' 1 -22
Convert-Sound 'scratching\scratching_metal_bolt.wav' 'scratch' 2 -22

Convert-Sound 'breathing\human_dry_crisp_breath_out.wav' 'breath' 1 -25
Convert-Sound 'breathing\human_hallway_reverb_breath.wav' 'breath' 2 -25
Convert-Sound 'breathing\human_ominous_breath.wav' 'breath' 3 -25
Convert-Sound 'breathing\human_whoosh_reverb_breath.wav' 'breath' 4 -26

Convert-Sound 'whispers\creepy_reverse_whispering_6seconds.wav' 'whisper' 1 -26
Convert-Sound 'whispers\glitchy_whispering.wav' 'whisper' 2 -27
Convert-Sound 'whispers\whispering_i_see_you.wav' 'whisper' 3 -26

Convert-Sound 'doors\door_opening_creeking.wav' 'door' 1 -21
Convert-Sound 'impacts\glass_clink.wav' 'impact' 1 -22
Convert-Sound 'footsteps\footstep_moon_impact_01.wav' 'impact' 2 -20
Convert-Sound 'ambience\duduk_drone_17seconds.wav' 'drone' 1 -30
Convert-Sound 'ambience\radio_loop_noise_feedback_buzzing_30seconds.wav' 'radio_signal' 1 -33
Convert-Sound 'singing\vocal_adlib_singing_wet.wav' 'distant_singing' 1 -32
Convert-Sound 'singing\vocal_harmony_vintage_reverb_soulchoir_23seconds.wav' 'distant_singing' 2 -33
Convert-Sound 'singing\vocal_reverb_femalesinging_distant.wav' 'distant_singing' 3 -33
Convert-Sound 'ambience\riser_drone_scary_41seconds.wav' 'rising_dread' 1 -34
Convert-Sound 'ambience\c_decending_drone_calm_53seconds.wav' 'deep_drone' 1 -35
Convert-Sound 'ambience\long_bass_drone_1min43seconds.wav' 'deep_drone' 2 -36

Write-Host 'Custom sound conversion complete.'
