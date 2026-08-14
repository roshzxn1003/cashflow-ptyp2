import re

with open('/app/applet/app/src/main/java/com/example/data/database/CashFlowDatabase.kt', 'r') as f:
    content = f.read()

# I need to match everything from "// Seed Initial Sample Transactions" down to the end of seedDefaultData
# which is "}"

match = re.search(r'(// Seed Initial Sample Transactions.*?)\s*\}\s*\}\s*\}', content, re.DOTALL)
if match:
    to_remove = match.group(1)
    content = content.replace(to_remove, '')

with open('/app/applet/app/src/main/java/com/example/data/database/CashFlowDatabase.kt', 'w') as f:
    f.write(content)
