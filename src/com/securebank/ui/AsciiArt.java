package com.securebank.ui;

/**
 * Creative ASCII Art Banners and visual design components for the Secure Banking Console.
 * Designed for visual clarity, elegance, and distinct terminal branding.
 *
 * @author Tinevimbo Musingadi (H250125B)
 */
public final class AsciiArt {

    private AsciiArt() {}

    public static final String MAIN_BANNER =
            "========================================================================================\n" +
            "  ██████╗ ███████╗ ██████╗██╗   ██╗██████╗ ███████╗    ██████╗  █████╗ ███╗   ██╗██╗  ██╗\n" +
            "  ██╔════╝ ██╔════╝██╔════╝██║   ██║██╔══██╗██╔════╝    ██╔══██╗██╔══██╗████╗  ██║██║ ██╔╝\n" +
            "  ███████╗█████╗  ██║     ██║   ██║██████╔╝█████╗      ██████╔╝███████║██╔██╗ ██║█████╔╝ \n" +
            "  ╚════██║██╔══╝  ██║     ██║   ██║██╔══██╗██╔══╝      ██╔══██╗██╔══██║██║╚██╗██║██╔═██╗ \n" +
            "  ██████╔╝███████╗╚██████╗╚██████╔╝██║  ██║███████╗    ██████╔╝██║  ██║██║ ╚████║██║  ██╗\n" +
            "  ╚═════╝ ╚══════╝ ╚═════╝ ╚═════╝ ╚═╝  ╚═╝╚══════╝    ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═══╝╚═╝  ╚═╝\n" +
            "                               CONSOLE BANKING SYSTEM                                   \n" +
            "                       [ Zero-Trust • PBKDF2 • RBAC • Audit ]                          \n" +
            "========================================================================================";

    public static final String VAULT_ICON =
            "                       .-----------------------------------.\n" +
            "                      /   .-----------------------------.   \\\n" +
            "                     |   /   _________________________   \\   |\n" +
            "                     |  |   |  __  __  _   _  ___ _____ |  |  |\n" +
            "                     |  |   | [  \\/  ]| | | |/ _ \\_   _]|  |  |\n" +
            "                     |  |   | [ |\\/| ]| |_| |  _  | | |  |  |  |\n" +
            "                     |  |   | [_|  |_]|_____/_| |_| |_|  |  |  |\n" +
            "                     |  |   |___________________________|  |  |\n" +
            "                     |  |             ( (O) )              |  |\n" +
            "                     |  |            /=======\\             |  |\n" +
            "                     |  |           | ===*=== |            |  |\n" +
            "                     |  |            \\=======/             |  |\n" +
            "                     |   \\                               /   |\n" +
            "                      \\   '-----------------------------'   /\n" +
            "                       '-----------------------------------'";

    public static final String SHIELD_SECURITY =
            "                                  .---.\n" +
            "                                 /_____\\\n" +
            "                                ( (@= ) )\n" +
            "                                 \\  =  /\n" +
            "                              .---'---'---.\n" +
            "                             /  SECURE-AUTH \\\n" +
            "                            |  [PBKDF2-HMAC] |\n" +
            "                            |   65k-HASHES   |\n" +
            "                             \\  LOCKOUT-ON  /\n" +
            "                              \\   3-TRIES  /\n" +
            "                               \\    ___   /\n" +
            "                                '-( V )-'\n" +
            "                                   '-'";

    public static final String SUCCESS_STAMP =
            "  +---------------------------------------------------+\n" +
            "  | [OK] TRANSACTION APPROVED & VERIFIED CRYPTOGRAPHICALLY |\n" +
            "  +---------------------------------------------------+";

    public static final String WARNING_STAMP =
            "  +---------------------------------------------------+\n" +
            "  | [!] SECURITY ALERT: TRANSACTION OR ACTION REJECTED |\n" +
            "  +---------------------------------------------------+";

    public static final String GOODBYE =
            "  ======================================================\n" +
            "     Thank you for banking with Secure Bank Systems.    \n" +
            "    Session terminated safely. Memory buffers wiped.    \n" +
            "  ======================================================";

    public static void printReceipt(String txId, String date, String type,
                                    String source, String dest, String amount, String balance) {
        System.out.println("  .--------------------------------------------------.");
        System.out.println("  |             OFFICIAL TRANSACTION RECEIPT         |");
        System.out.println("  |--------------------------------------------------|");
        System.out.printf("  | Ref ID       : %-33s |\n", txId);
        System.out.printf("  | Timestamp    : %-33s |\n", date);
        System.out.printf("  | Operation    : %-33s |\n", type);
        System.out.printf("  | From Account : %-33s |\n", source);
        System.out.printf("  | To / Channel : %-33s |\n", dest);
        System.out.printf("  | Amount       : $%-32s |\n", amount);
        System.out.printf("  | New Balance  : $%-32s |\n", balance);
        System.out.println("  | Status       : SUCCESS (Tamper-evident record)   |");
        System.out.println("  '--------------------------------------------------'");
    }
}
