/**
 *
 *
 *
 */
package realdeal;

import java.awt.event.KeyEvent;

/**
 *
 * Provides a hook for future editors to override.
 * Prolly better, nix abstract Editor, rename this Editor
 */
public class BasicEditor extends Editor {

    boolean useInsert = true;
    //CaretSys caret;// in abstract parent
    //int baseColor;

    private static char[] PADDING = {' ', ' ', ' ', ' ', ' '};
    private static final char SPACE = ' ';

    public BasicEditor() {

    }

    private boolean isLineEnd(char ch) {
        return ch == '\n' || ch == '\r';
    }
    public void toggleInsert() {
        useInsert = !useInsert;
    }
    public void arrows(int akey) {
        switch (akey) {
            case KeyBank.UP:
                caret.caretUp();
                break;
            case KeyBank.DOWN:
                caret.caretDown();
                break;
            case KeyBank.LEFT:
                caret.caretLeft();
                break;
            case KeyBank.RIGHT:
                caret.caretRight();
                break;
        }
    }
    public void symbol(char sym) {
        if (useInsert) {
            model.insert(caret.line, caret.column, baseColor, sym);
            
            caret.caretRight();
        } else {
            model.overwrite(caret.line, caret.column, baseColor, sym);
        }
    }
    public void setTabWidth(int blocks) {
        if (blocks > 0 && blocks != PADDING.length) {
            PADDING = new char[blocks];
            for (int i = 0; i < blocks; i++) {
                PADDING[i] = SPACE;
            }
        }
    }
    public void tab() {
        model.insert(caret.line, caret.column, PADDING);
        caret.moveCaret(caret.line, caret.column + PADDING.length);
    }
    public void backspace() {
        if (highlight.visible) {
            highlight.visible = false;//not very modular
            model.delete(highlight.startLine, highlight.startColumn, highlight.endLine, highlight.endColumn);
            int jumpLine = 0, jumpCol = 0;
            if (highlight.startLine == highlight.endLine) {
                jumpLine = highlight.startLine;
                jumpCol = highlight.startColumn <= highlight.endColumn ? highlight.startColumn : highlight.endColumn;
            } else if (highlight.startLine < highlight.endLine) {
                jumpLine = highlight.startLine;
                jumpCol = highlight.startColumn;
            } else {
                jumpLine = highlight.endLine;
                jumpCol = highlight.endColumn;
            }
            caret.moveCaret(jumpLine, jumpCol);
        } else {
            int line = caret.line;
            int col = caret.column;
            //System.out.println("Caret Prior = Line: " + caret.line + ", Col: " + caret.column);
            if (line == 0 && col == 0) {
                return;//no-op
            }            //Spot to delete is the position behind caret.
            int destcol = 0, destline = 0;
            if (col == 0) {//know line cant be zero
                destline = line - 1;
                //destcol = textbox.getLineWidth(destline) - 1;
                destcol = model.getLast(destline) + 1;
            } else {
                destline = line;
                destcol = col - 1;
            }
            //System.out.println("Decision to Delete: line: " + destline + ", col: " + destcol);
            model.delete(destline, destcol, destline, destcol);
            caret.moveCaret(destline, destcol);
        }
    }

    //@Override
    public void insertNewline() {
        model.insert(caret.line, caret.column, baseColor, '\n');
        caret.moveCaret(caret.line + 1, 0);
    }
    /**
     * All key presses enter here.
     * @param pressed
     * @param shift 
     */
    @Override
    public void handleKeyPress(int pressed, boolean shift) {
        //derail these now
        if (pressed == KeyBank.SHIFT || pressed == KeyBank.ALT || pressed == KeyBank.CONTROL) {
            return;
        }

        if (pressed >= 0x25 && pressed <= 0x28) {//Arrows
            arrows(pressed);
        } else if (pressed >= 0x41 && pressed <= 0x5a) {//LETTERS
            symbol((char) ((shift || KeyBank.capsLock) ? pressed : pressed + 0x20));
        } else if (pressed >= 0x30 && pressed <= 0x39) {//NUMBERS
            symbol(shift ? KeyBank.helpers[pressed - 0x30] : (char) pressed);
        } else if (pressed == KeyEvent.VK_ENTER) {
            insertNewline();
        } else if (pressed == KeyEvent.VK_BACK_SPACE) {
            backspace();
        } else if (KeyBank.extras.containsKey(pressed)) {//PUNCTUATION
            symbol(shift ? KeyBank.shiftExtras.get(pressed) : KeyBank.extras.get(pressed));
        } else if (pressed == 0x14) {
            KeyBank.capsLock = !KeyBank.capsLock;
        } else if (pressed >= 0x70 && pressed <= 0x7b) {// F1 thru F12
            //nothing yet
        } else if (pressed >= 0x60 && pressed <= 0x69) {//NumPad numbers
            symbol((char) (pressed - 0x60 + 48));
        } else if (pressed > 105 && pressed < 112) {//Numpad operators: Note: does not include 108 for some reason
            switch (pressed) {
                case 106: symbol('*'); break;
                case 107: symbol('+'); break;
                case 109: symbol('-'); break;
                case 110: symbol('.'); break;
                case 111: symbol('/'); break;
            }
        } else if (pressed >= 0x21 && pressed <= 0x24) {
                //0x21 pageup, 0x22 pagedown
            //0x23 end, 0x24 home
        } else if (pressed == KeyBank.TAB) {
            tab();
        } else if (pressed == KeyBank.INSERT) {
            toggleInsert();
        }
    }
}
