/**
 *
 * 
 *
 */

package realdeal;

import java.util.HashMap;

/**
 *
 * @author chris
 */
public class KeyBank {
    
    public static final int UP            = 0x26;
    public static final int RIGHT         = 0x27;
    public static final int DOWN          = 0x28;
    public static final int LEFT          = 0x25;
    
    public static final int HOME          = 0x24;
    public static final int END           = 0x23;
    public static final int PAGEUP        = 0x21;
    public static final int PAGEDOWN      = 0x22;
    
    public static final int SPACE          = 0x20;
    public static final int COMMA          = 0x2C;
    public static final int MINUS          = 0x2D;
    public static final int PERIOD         = 0x2E;
    public static final int FORWARD_SLASH  = 0x2F;
    public static final int SEMICOLON      = 0x3B;

    public static final int EQUALS         = 0x3D;
    public static final int OPEN_BRACKET   = 0x5B;// ]
    public static final int BACK_SLASH     = 0x5C;
    public static final int CLOSE_BRACKET  = 0x5D;// [
    
    public static final int F1             = 0x70;
    public static final int F2             = 0x71;
    public static final int F3             = 0x72;
    public static final int F4             = 0x73;
    public static final int F5             = 0x74;
    public static final int F6             = 0x75;
    public static final int F7             = 0x76;
    public static final int F8             = 0x77;
    public static final int F9             = 0x78;
    public static final int F10            = 0x79;
    public static final int F11            = 0x7A;
    public static final int F12            = 0x7B;
    
    public static final int UNDERSCORE     = 0x020B;
    public static final int COLON          = 0x0201;
    public static final int LESS           = 0x99;
    public static final int GREATER        = 0xa0;
    public static final int BRACELEFT      = 0xa1;
    public static final int BRACERIGHT     = 0xa2;
    public static final int INSERT         = 0x9B;
    public static final int DELETE         = 0x7F;
    public static final int NUM_LOCK       = 0x90;
    public static final int SCROLL_LOCK    = 0x91;
    
    public static final int SHIFT          = 0x10;
    public static final int CONTROL        = 0x11;
    public static final int ALT            = 0x12;
    public static final int ENTER          = '\n';
    public static final int BACK_SPACE     = '\b';
    public static final int TAB            = '\t';
    public static final int CANCEL         = 0x03;
    public static final int CLEAR          = 0x0C;
    public static final int PAUSE          = 0x13;
    public static final int CAPS_LOCK      = 0x14;
    public static final int ESCAPE         = 0x1B;
    
    public static final char[] helpers = { ')', '!', '@', '#', '$', '%', '^', '&', '*', '(' };
    
    public static boolean capsLock = false;
    
    public static final HashMap<Integer, Character> extras = new HashMap<>();
    public static final HashMap<Integer, Character> shiftExtras = new HashMap<>();
    
    static {
        extras.put(0x5b, '[');
        extras.put(0x5d, ']');
        extras.put(0x2c, ',');
        extras.put(0x20, ' ');
        extras.put(0x5c, '\\');
        extras.put(0x2e, '.');
        extras.put(0x2f, '/');
        extras.put(0x2d, '-');
        extras.put(0x3d, '=');
        extras.put(192, '`');
        extras.put(0x3b, ';');
        extras.put(222, '\'');
        
        shiftExtras.put(0x5b, '{');
        shiftExtras.put(0x5d, '}');
        shiftExtras.put(0x2c, '<');
        shiftExtras.put(0x20, ' ');
        shiftExtras.put(0x5c, '|');
        shiftExtras.put(0x2e, '>');
        shiftExtras.put(0x2f, '?');
        shiftExtras.put(0x2d, '_');
        shiftExtras.put(0x3d, '+');
        shiftExtras.put(192, '~');
        shiftExtras.put(0x3b, ':');
        shiftExtras.put(222, '"');
    }

    //static factory
    
}
