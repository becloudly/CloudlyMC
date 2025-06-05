# New Ideas

2. **Whitelist Application System:**
    * Instead of direct adding, players could use an in-game command (e.g., `/whitelist apply <reason>`) to request access.
    * Admins would get a notification and could review applications (e.g., `/whitelist admin review` > Opens a nice designed and intuitive GUI (with heads, reason, green/red block to decide etc.)) and approve or deny them with a reason.
    * If they aren't on the whitelist, they are frozen in any way, so they can't play until approved.

4. **Discord Whitelist Management:**
    * Allow users to join the server and do a verification process.
        - The join the server and have to enter a command (e.g., `/whitelist verify <username>`).
    * This command would check if the username exists on the discord server and have a specific role.
    * Changes made via Discord (like change role/ban users) would sync with the in-game whitelist dynamicly and in real-time.

5. **Whitelist Vouchers/Tokens:**
    * Admins can generate one-time use codes or tokens (like for events) (e.g., `/whitelist admin createtoken <player>` with a clickable response to copy the token to the clipboard).
    * Players can redeem these tokens (e.g., `/whitelist redeem <code>`) to get whitelisted. Useful for giveaways or granting access to specific individuals without needing their username immediately.

6. **Whitelist Full Notification & Reason:**
    * When the server is full and a whitelisted player tries to join the server, they receive a notification that the server is full.
    * When a non-whitelisted player tries to join (and a feature like tokens, discord or application is not used), they receive a message indicating that they are not whitelisted and the join is blocked.
    * If one of the features is used, they can join but are freezed and receive a message indicating that they need use one of these activated features.