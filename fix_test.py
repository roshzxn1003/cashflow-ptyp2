import re

with open('/app/applet/app/src/test/java/com/example/ExampleRobolectricTest.kt', 'r') as f:
    content = f.read()

content = content.replace('assertEquals("My Application", appName)', 'assertEquals("CashFlow", appName)')

with open('/app/applet/app/src/test/java/com/example/ExampleRobolectricTest.kt', 'w') as f:
    f.write(content)
