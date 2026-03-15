package org.dreambot.api.script;

import java.io.File;
import java.awt.Graphics;

public abstract class AbstractScript {
    public File getDirectory() { return new File("."); }
    public void stop() { /* stub */ }
    public void onStart() { /* stub */ }
    public void onExit() { /* stub */ }
    public void onPaint(Graphics g) { /* stub */ }
    public abstract int onLoop();
}
