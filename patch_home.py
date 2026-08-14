import re

with open('/app/applet/app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'r') as f:
    content = f.read()

# I will find the block starting with "// Voice AI Banner Trigger" and ending before "// Quick Actions Row"
start_idx = content.find('        // Voice AI Banner Trigger')
if start_idx != -1:
    end_idx = content.find('        // Quick Actions Row', start_idx)
    if end_idx != -1:
        content = content[:start_idx] + content[end_idx:]

with open('/app/applet/app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'w') as f:
    f.write(content)
