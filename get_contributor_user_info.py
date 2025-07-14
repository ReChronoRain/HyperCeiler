import os
import requests
from pathlib import Path
from time import sleep
from PIL import Image
from io import BytesIO
from typing import Optional
import time
import urllib.parse

# Tuple of GitHub login ID to string resource ID
GITHUB_USERS = [
    ("Adlyq", "contributors_adlyq"),
    ("CC0126", "contributors_cc0126"),
    ("CECoffee", "contributors_cecoffee"),
    ("DeepChirp", "contributors_deepchirp"),
    ("ItzDFPlayer", "contributors_itzdfplayer"),
    ("heinu123", "contributors_heinu123"),
    ("Fan095", "contributors_fan095"),
    ("frank-782", "contributors_frank-782"),
    ("GMerge01", "contributors_GMerge01"),
    ("GSWXXN", "contributors_GSWXXN"),
    ("ghhccghk", "contributors_ghhccghk"),
    ("hamjin", "contributors_hamjin"),
    ("Horange321", "contributors_Horange321"),
    ("hrsthrt74", "contributors_hrsthrt74"),
    ("igormiguell", "contributors_igormiguell"),
    ("LiuBodong", "contributors_LiuBodong"),
    ("LuoYunXi0407", "contributors_LuoYunXi0407"),
    ("Memory2314", "contributors_Memory2314"),
    ("NahidaBuer", "contributors_NahidaBuer"),
    ("nakixii", "contributors_nakixii"),
    ("Henvy-Mango", "contributors_Henvy_Mango"),
    ("NextAlone", "contributors_NextAlone"),
    ("nxibjzC", "contributors_nxibjzC"),
    ("oufm", "contributors_oufm"),
    ("pzcn", "contributors_pzcn"),
    ("qdsp6sw", "contributors_qdsp6sw"),
    ("ri-char", "contributors_ri_char"),
    ("FurryRbl", "contributors_FurryRbl"),
    ("SmartJQ", "contributors_SmartJQ"),
    ("SpaceVector", "contributors_SpaceVector"),
    ("yxsra", "contributors_yxsra"),
    ("v5u871", "contributors_v5u871"),
    ("Voemp", "contributors_Voemp"),
    ("hosizoraru", "contributors_hosizoraru"),
    ("Wansn-w", "contributors_wansn-w"),
    ("weigui404", "contributors_weigui404"),
    ("Weverses", "contributors_Weverses"),
    ("wushidia", "contributors_wushidia"),
    ("Xander-C", "contributors_Xander-C"),
    ("xueshiji", "contributors_xueshiji"),
    ("xzakota", "contributors_xzakota"),
    ("YifePlayte", "contributors_YifePlayte"),
    ("YunZiA", "contributors_YunZiA"),
    ("Nep-Timeline", "contributors_Nep_Timeline"),
    ("zcarroll4", "contributors_zcarroll4"),
    ("lightsummer233", "contributors_lightsummer233"),
    ("HChenX", "contributors_HChenX"),
    ("klxiaoniu", "contributors_klxiaoniu"),
    ("lingqiqi5211", "contributors_lingqiqi5211"),
    ("HolyBearTW", "contributors_HolyBearTW"),
    ("Meetingfate", "contributors_Meetingfate"),
    ("xing0meng", "contributors_xing0meng"),
    ("lswlc33", "contributors_lswlc33"),
    ("zjw2017", "contributors_zjw2017"),
    ("YuKongA", "contributors_YuKongA"),
    ("pomelohan", "contributors_pomelohan"),
    ("CLOUDERHEM","contributors_CLOUDERHEM"),
    ("Voyager","contributors_Voyager"),
    ("xiefei-github","contributors_xiefei_github"),
    ("tehcneko","contributors_tehcneko"),
    ("hosizoraru","contributors_hosizoraru")
]

DRAWABLE_DIR = "library/common-ui/java/main/res/drawable"
OUTPUT_KT = "library/common-ui/java/main/java/org/akanework/gramophone/logic/utils/data/Contributors.kt"
API_BASE = "https://api.github.com/users/"

HEADERS = {
    "Accept": "application/vnd.github+json",
}

try:
    with open("fastlane/creds.txt", "r", encoding="utf-8") as f:
        HEADERS["Authorization"] = "Bearer " + f.read().strip()
except Exception as e:
    print("Not using auth, may be subject to rate limits")

def sanitize_login(login: str) -> str:
    return ''.join(c if c.isalnum() else '_' for c in login.lower())

def fetch_user_data(login: str) -> Optional[dict]:
    url = f"{API_BASE}{login}"
    try:
        response = requests.get(url, headers=HEADERS, timeout=10)
        response.raise_for_status()
        return response.json()
    except Exception as e:
        print(f"❌ get users error {login}: {e}")
        return None

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
    for user in GITHUB_USERS:
        login = user[0]
        print(f"📦 Processing users: {login}")
        user_data = fetch_user_data(login)
        if not user_data:
            continue

        filename = f"contributor_{sanitize_login(login)}"
        avatar_url = user_data.get("avatar_url", "")
        download_and_save_avatar(avatar_url, filename)

if __name__ == "__main__":
    main()
