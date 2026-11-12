# TikTok Telegram Downloader Bot

## Deploy
1. Ku dar token-ka Telegram Railway Variables magaca `TELEGRAM_BOT_TOKEN` (ha gelin token-ka source code).
2. Railway wuxuu rakibayaa `requirements.txt`, kadibna wuxuu bilaabayaa `python bot.py`.
3. Telegram ka fur bot-ka oo dir `/start`.

## Features
- Download queue iyo xaddididda downloads-ka isku mar socda.
- Horumarka download-ka oo Telegram lagu tuso.
- Tayada video-ga: `/quality high`, `/quality medium`, ama `/quality low`.
- Xakamaynta spam-ka user kasta.
- Khaladaad si cad loo maareeyo.
- Tirakoob admin oo lagu furo `/stats`.

## Optional Railway variables
- `ADMIN_IDS`: Telegram numeric user IDs oo comma-separated ah, tusaale `123456789,987654321`. Ku dar ID-gaaga si aad u isticmaasho `/stats`.
- `MAX_CONCURRENT_DOWNLOADS`: downloads isku mar socda; default `2`.
- `MAX_QUEUE_SIZE`: tirada codsiyada safka iyo kuwa sugaya; default `20`.
- `USER_COOLDOWN_SECONDS`: inta ilbiriqsi ee user-ku sugo codsi kale; default `15`.
- `LOG_LEVEL`: log level; default `INFO`.

**Fiiro gaar ah:** Tirakoobka hadda wuxuu ku jiraa xusuusta process-ka; wuu eber noqonayaa marka bot-ka la restart-gareeyo ama redeploy la sameeyo. Haddii aad rabto tirakoob joogto ah, ku xiro database/volume persistent ah.

Isticmaal kaliya muuqaallo aad leedahay ama aad fasax u haysato inaad soo dejisato. Bot-ku ma dhaafo private/restricted content.
