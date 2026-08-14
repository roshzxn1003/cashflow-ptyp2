import re

with open('/app/applet/app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

content = content.replace('onProcessPrompt = { prompt -> viewModel.processVoicePrompt(prompt) },', 'onProcessPrompt = { prompt -> viewModel.processVoicePrompt(prompt) },\n                onProcessAudio = { audioBase64 -> viewModel.processAudioPrompt(audioBase64) },')

with open('/app/applet/app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
