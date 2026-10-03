import re
html = open('/tmp/hub_match_result.html', encoding='utf-8', errors='replace').read()
pcts = re.findall(r'podobienstwo:\s*(\d+)%', html)
print("ALL_PCTS:", pcts)
titles = re.findall(r'<h2[^>]*>(.*?)</h2>', html, re.S)
flat = [re.sub(r'<[^>]+>', '', t).strip() for t in titles]
flat = [t for t in flat if t]
print("TITLES:", flat[:6])