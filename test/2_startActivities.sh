echo "Starting MainActivity"
adb -e shell am start -n "com.appenjoyment.lfnw/.MainActivity"

# WebViewActivity with contenti
echo "Starting WebViewActivity with Venue content"
adb -e shell am start -n "com.appenjoyment.lfnw/.WebViewActivity" -e "KEY_URL" "http://linuxfestnorthwest.org/information/venue"

echo "Starting WebViewActivity with Venue content"
adb -e shell am start -n "com.appenjoyment.lfnw/.WebViewActivity" -e "KEY_URL" "http://linuxfestnorthwest.org/sponsors"

echo "Starting WebViewActivity with Venue content"
adb -e shell am start -n "com.appenjoyment.lfnw/.WebViewActivity" -e "KEY_URL" "http://www.linuxfestnorthwest.org/node/2977/cod_registration"

echo "Starting WebViewActivity with Venue content"
adb -e shell am start -n "com.appenjoyment.lfnw/.WebViewActivity" -e "KEY_URL" "http://www.linuxfestnorthwest.org/node/2977/cod_registration"

echo "Starting WebViewActivity with custom content, nooooooooo!"
adb -e shell am start -n "com.appenjoyment.lfnw/.WebViewActivity" -e "KEY_URL" "http://www.apple.com/iphone/"
