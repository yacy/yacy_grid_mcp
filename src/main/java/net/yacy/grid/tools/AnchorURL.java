/**
 *  AnchorURL
 *  Copyright 2013 by Michael Peter Christen @orbiterlab
 *  first published 15.09.2013 on http://yacy.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program in the file lgpl21.txt
 *  If not, see <http://www.gnu.org/licenses/>.
 */


package net.yacy.grid.tools;

import java.net.MalformedURLException;
import java.util.Properties;

public class AnchorURL extends MultiProtocolURL {

    /*
     * the 'new' AnchorURL class is a fork of the original yacy.cora.document.id.AnchorURL
     * class, but depends on MultiProtocolURL rather than DigestURL, which is removed for
     * YaCy Grid.
     */

    private static final long serialVersionUID = 1586579902179962086L;

    private String nameProperty, relProperty, textBody; // may contain additional url properties, such as given in html a href-links
    private MultiProtocolURL imageURL; // in case that the anchor contains an image link, store image url; if there is no image then set this to null
    private String imageAlt; // in case that the anchor contains an image link, store the alt property; if there is no image then set this to null

    public AnchorURL(final String url) throws MalformedURLException {
        super(url);
        this.textBody = "";
        this.nameProperty = "";
        this.relProperty = "";
        this.imageURL = null;
        this.imageAlt = null;
    }

    public AnchorURL(final MultiProtocolURL url) {
        super(url);
        this.textBody = "";
        this.nameProperty = "";
        this.relProperty = "";
        this.imageURL = null;
        this.imageAlt = null;
    }

    public AnchorURL(final MultiProtocolURL baseURL, final String relPath) throws MalformedURLException {
        super(baseURL, relPath);
        this.textBody = "";
        this.nameProperty = "";
        this.relProperty = "";
        this.imageURL = null;
        this.imageAlt = null;
    }

    public AnchorURL(final String protocol, final String host, final int port, final String path) throws MalformedURLException {
        super(protocol, host, port, path);
        this.textBody = "";
        this.nameProperty = "";
        this.relProperty = "";
        this.imageURL = null;
        this.imageAlt = null;
    }

    public AnchorURL(final String protocol, final String host, final int port, final String path, final MultiProtocolURL imageURL, final String imageAlt) throws MalformedURLException {
        super(protocol, host, port, path);
        this.textBody = "";
        this.nameProperty = "";
        this.relProperty = "";
        this.imageURL = imageURL;
        this.imageAlt = imageAlt;
    }

    public static AnchorURL newAnchor(final MultiProtocolURL baseURL, String relPath) throws MalformedURLException {
        if (relPath.startsWith("//")) {
            // patch for urls starting with "//" which can be found in the wild
            relPath = (baseURL == null) ? "http:" + relPath : baseURL.getProtocol() + ":" + relPath;
        }
        if ((baseURL == null) ||
            isHTTP(relPath) ||
            isHTTPS(relPath) ||
            isS3(relPath) ||
            isFTP(relPath) ||
            isFile(relPath) ||
            isSMB(relPath) ||
            relPath.startsWith("mailto:")) {
            return new AnchorURL(relPath);
        }
        return new AnchorURL(baseURL, relPath);
    }

    public String getNameProperty() {
        return this.nameProperty;
    }

    public void setNameProperty(String name) {
        this.nameProperty = name;
    }

    public String getTextProperty() {
        return this.textBody;
    }

    public void setTextProperty(String text) {
        this.textBody = text;
    }

    public String getRelProperty() {
        return this.relProperty;
    }

    public void setRelProperty(String rel) {
        this.relProperty = rel;
    }

    public MultiProtocolURL getImageURL() {
        return this.imageURL;
    }

    public void setImageURL(MultiProtocolURL imageURL) {
        this.imageURL = imageURL;
    }

    public String getImageAlt() {
        return this.imageAlt;
    }

    public void setImageAlt(String imageAlt) {
        this.imageAlt = imageAlt;
    }

    public void setAll(final Properties tagopts) {
        this.nameProperty = tagopts.getProperty("name", "");
        this.textBody = tagopts.getProperty("text", "");
        this.relProperty = tagopts.getProperty("rel", "");
    }

    public Properties getAll() {
        final Properties tagopts = new Properties();
        tagopts.setProperty("name", this.nameProperty);
        tagopts.setProperty("text", this.textBody);
        tagopts.setProperty("rel", this.relProperty);
        return tagopts;
    }

    public boolean attachedNofollow() {
        return this.relProperty.indexOf("nofollow") >= 0;
    }

    public String toHTML() {
        return "<a href=\"" + this.toNormalform(false) + "\"" +
                (this.nameProperty.length() > 0 ? (" name=\"" + this.nameProperty + "\"") : "") +
                (this.relProperty.length() > 0 ? (" rel=\"" + this.relProperty + "\"") : "") +
                ">" + this.textBody + "</a>";
    }

}
