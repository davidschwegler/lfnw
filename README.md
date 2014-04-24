LinuxFest Northwest
====

The Official LinuxFest Android App

Get the published version in the Play Store!
https://play.google.com/store/apps/details?id=com.appenjoyment.lfnw

Todo
====
* Developer features
* User Features
  * Replace web-views with in-app content
  * Improved directions and venue info -- some basic offline info here, and the ability to open Google Maps to the venue location
  * Notes - take notes on the presentation
  * In-app presentation description - when you're in the building, you don't always have the best network connection, so loading the web page with the presentation info can be painful. Having a JSON or XML file I can pull down containing the summary would be amazing. I can probably parse it out of the sessions' webpage and cache it, though it would be slower.
  * Campus map -- show where the presentation is on the campus floor
  * Shared comments - live comment on the presentation with other users. An IRC-like system would work, though it would be cool to hook into the comment system on the website...but that would probably require in-app sign-in, and so would require some help from your web person. 
  * In-app sign in and registration - for shared comments
* Code features
  * Use Calendar object rather than deprecated Date
  * Use ContentProvider for sessions db
  * Use CursorLoader (?) to avoid calling requery() on the sessions list
