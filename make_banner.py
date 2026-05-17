import sys, os, time
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.chrome.service import Service
from webdriver_manager.chrome import ChromeDriverManager

html_path = os.path.abspath("banner_source.html")
output_path = os.path.abspath("banner.png")

options = Options()
options.add_argument("--headless")
options.add_argument("--no-sandbox")
options.add_argument("--disable-dev-shm-usage")
options.add_argument("--window-size=1200,400")
options.add_argument("--hide-scrollbars")
options.add_argument("--disable-gpu")
options.add_argument("--force-device-scale-factor=2")  # 2x resolution = crisp

driver = webdriver.Chrome(
    service=Service(ChromeDriverManager().install()),
    options=options
)

driver.set_window_size(1200, 400)
driver.get(f"file:///{html_path}")
time.sleep(2)  # wait for fonts to load

driver.save_screenshot(output_path)
driver.quit()

print(f"Banner saved to: {output_path}")
