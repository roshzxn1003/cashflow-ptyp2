with open('/app/applet/gradle/libs.versions.toml', 'r') as f:
    content = f.read()

content = content.replace("[versions]\n", "[versions]\nsupabase = \"3.0.1\"\nktor = \"3.0.1\"\n")

with open('/app/applet/gradle/libs.versions.toml', 'w') as f:
    f.write(content)
