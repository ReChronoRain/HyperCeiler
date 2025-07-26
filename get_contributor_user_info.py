import os
import requests
from pathlib import Path
from time import sleep
from PIL import Image
from io import BytesIO
from typing import Optional
import time
import urllib.parse


DRAWABLE_DIR = "library/common-ui/java/main/res/drawable"
API_BASE = "https://api.github.com/users/"
OUTPUT_KT = "library/common-ui/java/main/src/com/sevtinge/hyperceiler/common/data/Contributors.kt"
REPO = "ReChronoRain/HyperCeiler"
URL = f"https://api.github.com/repos/{REPO}/contributors?per_page=100"


HEADERS = {
    "Accept": "application/vnd.github+json",
}
def read_property(file_path: str, key: str) -> Optional[str]:
    try:
        with open(file_path, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if not line or line.startswith("#"):
                    continue
                if "=" in line:
                    k, v = line.split("=", 1)
                    if k.strip() == key:
                        return v.strip()
    except FileNotFoundError:
        print(f"文件 {file_path} 未找到")
    return None

try:
    gpr_key = read_property("signing.properties", "gpr.key")
    HEADERS["Authorization"] = "Bearer " + gpr_key
except Exception as e:
    print(f"Not using auth, may be subject to rate limits, error code {e}")

def sanitize_login(login: str) -> str:
    return ''.join(c if c.isalnum() else '_' for c in login.lower())

def get_user_profile(login: str) -> dict:
    url = f"{API_BASE}{login}"
    response = requests.get(url, headers=HEADERS)
    if response.status_code == 200:
        return response.json()
    else:
        print(f"Failed to fetch user profile for {login}: {response.status_code}")
        return {}

def fetch_contributors():
    response = requests.get(URL, headers=HEADERS)
    if response.status_code != 200:
        print(f"Error: {response.status_code} - {response.text}")
        return []

    contributors = response.json()
    return contributors


def download_and_save_avatar(url: str, filename: str):
    response = requests.get(url, timeout=10)
    response.raise_for_status()
    img = Image.open(BytesIO(response.content)).convert("RGBA")
    img.thumbnail((256, 256))
    os.makedirs(DRAWABLE_DIR, exist_ok=True)
    filepath = os.path.join(DRAWABLE_DIR, f"{filename}.webp")
    img.save(filepath, format="WebP", quality=50, method=6)
    print(f"✅ download ok: {filepath}")

def main():
    result = """// ===== 请不要编辑，此文件为自动生成 =====
// 使用 get_contributor_user_info.py 来生成此文件

package com.sevtinge.hyperceiler.common.data

import android.net.Uri
import com.sevtinge.hyperceiler.ui.R

object Contributors {
    private fun decode(text: String) = text.let { Uri.decode(it) }
    val LIST = listOf("""


    users = fetch_contributors()
    for user in users:
        login = user["login"].lower()
        type = user["type"].lower()
        if type != "bot" and login not in ["sevtinge", "crowdin-bot"]:
            print(f"✅ Get user ok: {login}，type：{type}")
            filename = f"contributor_{sanitize_login(login)}"
            nickname = get_user_profile(login).get("name", login) or login
            name = ("\"" + urllib.parse.quote(nickname) + "\"")
            print(f"✅ Get nickname ok: {nickname}，login: {login}")
            avatar_url = user["avatar_url"]
            download_and_save_avatar(avatar_url, filename)
            result += f"\n        GitHubUser(login = \"{login}\", name = decode({name}), avatar = R.drawable.{filename}),"

    result += "\n    )\n}\n"
    with open(OUTPUT_KT, "w", encoding="utf-8") as f:
        f.write(result)

    print(f"\n✅ All user processing is complete and results have been saved to {OUTPUT_KT}")

if __name__ == "__main__":
    main()
