with open('/app/applet/gradle/libs.versions.toml', 'r') as f:
    lines = f.readlines()

new_lines = []
for line in lines:
    if line.strip() == "supabase = \"3.0.1\"" or line.strip() == "ktor = \"3.0.1\"" or line.strip() == "ktor-client-core = \"3.0.1\"":
        continue
    new_lines.append(line)

with open('/app/applet/gradle/libs.versions.toml', 'w') as f:
    f.writelines(new_lines)
