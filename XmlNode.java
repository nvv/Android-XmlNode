import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Utility class for quick and simple xml serialization and deserialization.
 */
public class XmlNode {

    // node name/value.
    private String mName = "";
    private String mValue = "";

    /**
     * Initialization marker for node. it's <code>true</code> when node is created, but not initialized.
     * It's essential for cases when some nodes in your xml may be present or maybe absent, but you don't want to deal with null values.
     */
    private boolean mNullNode = true;

    private XmlNode mParent = null;

    private HashMap<String, String> mAttributes;

    private ArrayList<XmlNode> mChildren;
    private HashMap<String, Integer> mChildrenMap;

    public XmlNode() {
        mAttributes = new HashMap<String, String>();

        mChildren = new ArrayList<XmlNode>();
        mChildrenMap = new HashMap<String, Integer>();
    }

    public XmlNode(File file) throws XmlPullParserException, IOException {
        this();
        parse(file);
    }

    public boolean parse(File file) {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);

            XmlPullParser xpp = factory.newPullParser();
            XmlNode currentNode = this;

            xpp.setInput(new FileInputStream(file), "utf-8");
            int eventType = xpp.getEventType();

            boolean firstTag = true;

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (currentNode == null) {
                    continue;
                }
                // Parsing start tag
                if (eventType == XmlPullParser.START_TAG) {
                    XmlNode node;
                    if (firstTag) {
                        node = this;
                    } else {
                        node = new XmlNode();
                    }

                    node.setName(xpp.getName());
                    if (!firstTag) {
                        currentNode.addChild(node);
                    }

                    for (int counter = 0; counter < xpp.getAttributeCount(); counter++) {
                        node.setAttribute(xpp.getAttributeName(counter), xpp.getAttributeValue(counter));
                    }

                    currentNode = node;
                    firstTag = false;
                }
                // Parsing tag content (text)
                if (eventType == XmlPullParser.TEXT) {
                    currentNode.setValue(xpp.getText());
                }
                // Parsing end tag
                if (eventType == XmlPullParser.END_TAG) {
                    currentNode = currentNode.getParent();
                }

                eventType = xpp.next();
            }
        } catch (Exception exc) {
            return false;
        }
        return true;
    }

    private boolean serialize(XmlSerializer serializer) {
        try {
            serializer.startTag("", mName);
            for (String attrKey : mAttributes.keySet()) {
                serializer.attribute("", attrKey, mAttributes.get(attrKey));
            }
            for (XmlNode aMChildren : mChildren) {
                aMChildren.serialize(serializer);
            }
            if (mChildren.size() == 0) {
                serializer.text(mValue);
            }
            serializer.endTag("", mName);
            if (mParent == null) {
                serializer.endDocument();
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String toString() {
        XmlSerializer serializer = Xml.newSerializer();
        StringWriter writer = new StringWriter();
        try {
            serializer.setOutput(writer);
        } catch (Exception exc) {
            return "";
        }

        if (serialize(serializer)) {
            return writer.toString();
        } else {
            return "";
        }
    }

    public void setName(String name) {
        this.mName = name;
    }

    public String getName() {
        return mName;
    }

    public void setValue(String value) {
        this.mValue = value;
    }

    public String getValue() {
        return mValue;
    }

    public void setParent(XmlNode parent) {
        if (null != parent) {
            parent.addChild(this);
        } else {
            this.mParent = null;
        }
    }

    public XmlNode getParent() {
        return mParent;
    }

    public boolean isNullNode() {
        return mNullNode;
    }

    public void setAttribute(String attrName, String attrValue) {
        mAttributes.put(attrName, attrValue);
    }

    public void eraseAttribute(String attrName) {
        mAttributes.remove(attrName);
    }

    public String getAttribute(String attrName) {
        return mAttributes.get(attrName);
    }

    public long getAttributeLongValue(String attrName) {
        try {
            return Long.parseLong(mAttributes.get(attrName));
        } catch (NumberFormatException nfe) {
            return 0;
        }
    }

    public int getAttributeIntValue(String attrName) {
        try {
            return Integer.parseInt(mAttributes.get(attrName));
        } catch (NumberFormatException nfe) {
            return 0;
        }
    }

    public boolean getAttributeBooleanValue(String attrName) {
        return Boolean.parseBoolean(mAttributes.get(attrName));
    }

    public void addChild(XmlNode child) {
        child.mParent = this;
        mChildren.add(child);
        mNullNode = false;
        if (!mChildrenMap.containsKey(child.getName())) {
            mChildrenMap.put(child.getName(), mChildren.size() - 1);
        }
    }

    public void addChild(String name, String value) {
        XmlNode node = new XmlNode();
        node.mName = name;
        node.mValue = value;
        node.mNullNode = false;
        addChild(node);
    }

    public boolean isChildExists(String name) {
        return mChildrenMap.containsKey(name);
    }

    public XmlNode getChild(String name) {
        if (mChildrenMap.containsKey(name)) {
            return mChildren.get(mChildrenMap.get(name));
        }
        return new XmlNode();
    }

    public boolean hasChild() {
        return !mChildren.isEmpty();
    }

    public XmlNode getChild(int index) {
        return mChildren.get(index);
    }

    public ArrayList<XmlNode> getChildren() {
        return mChildren;
    }

    public int getChildrenCount() {
        return mChildren.size();
    }

    public String getChildValue(String name) {
        return getChild(name).getValue();
    }

    /**
     * Find node recursive.
     *
     * @param name node name.
     * @return node with specified name or default not initialized instance of <code>XmlNode</code>.
     */
    public XmlNode findNode(String name) {
        if (mChildrenMap.containsKey(name)) {
            return getChild(name);
        }

        for(XmlNode child : mChildren) {

            if (!child.hasChild()) {
                continue;
            }

            XmlNode xmlNode = child.findNode(name);
            if (!xmlNode.isNullNode()) {
                return xmlNode;
            }
        }

        return new XmlNode();
    }

    public long getChildLongValue(String name) {
        try {
            return Long.parseLong(getChildValue(name));
        } catch (NumberFormatException nfe) {
            return 0;
        }
    }

    public int getChildIntValue(String name) {
        try {
            return Integer.parseInt(getChildValue(name));
        } catch (NumberFormatException nfe) {
            return 0;
        }
    }

    public boolean getChildBooleanValue(String name) {
        return Boolean.parseBoolean(getChildValue(name));
    }

}

