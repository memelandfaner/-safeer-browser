# Safeer Mobile 1.0.16

YouTube keeps playing:

- No more "Video paused. Continue watching?": YouTube arms this prompt for every playback and cancels it only on its own activity signal (the one a real tap or key produces); refreshing the last-activity timestamp, as before, was not enough for every account. While a video or YouTube Music plays, the browser now reports activity through that signal every 20 seconds (driven by the media clock, so it also works with the screen off or the browser in the background), and the scheduled warning, dialog and pause are dropped before they can appear. The same signal keeps autoplay from pausing after several videos.
- Should the dialog still show up, it is recognised by what it is (a single "Yes" button, or its text in Safeer's languages) and confirmed, and the video resumes; dialogs that ask a real question (with a cancel button) are left alone.
- The background-playback notification shows a proper small icon instead of a blank square.

Everything runs on the phone; nothing is sent anywhere.

Slovensko: YouTube in YouTube Music se ne ustavljata več z »Video paused. Continue watching?« (»Predvajanje je zaustavljeno. Želite nadaljevati?«). YouTube to opozorilo pripravi ob vsakem predvajanju in ga prekliče le ob lastnem signalu aktivnosti, kot ga sproži pravi dotik; zgolj osveževanje časa zadnje aktivnosti pri vseh računih ni zadoščalo. Brskalnik med predvajanjem zdaj vsakih 20 sekund poroča aktivnost po tej poti (vezano na uro predvajanja, zato deluje tudi z ugasnjenim zaslonom ali v ozadju), zato do opozorila in premora sploh ne pride. Če bi se okno kljub temu pokazalo, ga brskalnik prepozna in potrdi. Obvestilo za predvajanje v ozadju ima zdaj pravo ikono.
