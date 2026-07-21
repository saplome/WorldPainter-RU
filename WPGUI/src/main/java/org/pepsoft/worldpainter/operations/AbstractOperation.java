/*
 * This file is part of WorldPainter Languages, an unofficial localization
 * fork of WorldPainter (https://github.com/saplome/WorldPainter-LANGUAGES).
 *
 * Original work Copyright © pepsoft.org, The Netherlands.
 * Modifications Copyright © 2026 saplome. This file was modified in 2026.
 *
 * This file remains licensed under the GNU General Public License,
 * version 3. See the LICENSE file for details.
 */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.operations;

import org.pepsoft.util.IconUtils;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.WorldPainterView;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.beans.PropertyVetoException;

/**
 * An abstract base class for WorldPainter {@link Operation}s which provides
 * name and description getters and separate {@link #activate()} and
 * {@link #deactivate()} methods for convenience, and implements
 * {@link #toString()}.
 *
 * @author pepijn
 */
public abstract class AbstractOperation implements Operation {
    protected AbstractOperation(String name, String description) {
        this(name, description, name.toLowerCase().replaceAll("\\s", ""));
    }

    protected AbstractOperation(String name, String description, String iconName) {
        if ((name == null) || (description == null)) {
            throw new NullPointerException();
        }
        this.name = name;
        this.description = description;
        this.iconName = iconName;
        icon = IconUtils.loadScaledImage(getClass().getClassLoader(), "org/pepsoft/worldpainter/icons/" + iconName + ".png");
    }

    @Override
    public void setView(WorldPainterView view) {
        this.view = view;
    }

    @Override
    public final String getName() {
        final String nk = "operation." + getClass().getSimpleName() + ".name";
        final String nv = org.pepsoft.worldpainter.WPI18n.s(nk);
        return nv.equals(nk) ? name : nv;
    }

    @Override
    public final String getDescription() {
        final String dk = "operation." + getClass().getSimpleName() + ".description";
        final String dv = org.pepsoft.worldpainter.WPI18n.s(dk);
        return dv.equals(dk) ? description : dv;
    }

    @Override
    public final boolean isActive() {
        return active;
    }

    @Override
    public final void setActive(boolean active) throws PropertyVetoException {
        if (active != this.active) {
            this.active = active;
            if (active) {
                try {
                    activate();
                } catch (PropertyVetoException e) {
                    this.active = false;
                    throw e;
                }
            } else {
                deactivate();
            }
        }
    }

    @Override
    public final BufferedImage getIcon() {
        final Object configuredTheme = UIManager.get("WorldPainter.iconTheme");
        if (configuredTheme instanceof String) {
            final String iconTheme = (String) configuredTheme;
            if (! iconTheme.equals(themedIconTheme)) {
                themedIcon = IconUtils.loadScaledImage(getClass().getClassLoader(),
                        "org/pepsoft/worldpainter/icons/" + iconTheme + "/" + iconName + ".png");
                themedIconTheme = iconTheme;
            }
            if (themedIcon != null) {
                return themedIcon;
            }
        }
        return icon;
    }

    @Override
    public JPanel getOptionsPanel() {
        return null;
    }

    @Override
    public String toString() {
        return getName();
    }

    protected final WorldPainterView getView() {
        return view;
    }

    /**
     * Utility method for obtaining the current dimension from the configured view.
     *
     * @return The current dimension from the configured view. May be {@code null} if the view is not configured, or it
     * does not return a dimension.
     */
    protected final Dimension getDimension() {
        return (view != null) ? view.getDimension() : null;
    }

    protected abstract void activate() throws PropertyVetoException;
    protected abstract void deactivate();

    private final String name, description, iconName;
    private final BufferedImage icon;
    private transient BufferedImage themedIcon;
    private transient String themedIconTheme;
    private boolean active;
    private WorldPainterView view;
}