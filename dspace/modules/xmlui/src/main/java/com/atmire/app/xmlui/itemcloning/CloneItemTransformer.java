package com.atmire.app.xmlui.itemcloning;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.content.Item;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 19 Jun 2018
 */
public class CloneItemTransformer extends AbstractDSpaceTransformer {

    private static final Message T_dspace_home = message("xmlui.general.dspace_home");
    private static final Message T_title = message("xmlui.itemcloning.CloneItemTransfomer.title");
    private static final Message T_trail = message("xmlui.itemcloning.CloneItemTransfomer.trail");
    private static final Message T_not_found = message
            ("xmlui.itemcloning.CloneItemTransfomer.not-found");
    private static final Message T_not_authorized = message
            ("xmlui.itemcloning.CloneItemTransfomer.not-authorized");
    private static final Message T_onderdeel_button = message
            ("xmlui.itemcloning.CloneItemTransfomer.onderdeel");
    private static final Message T_version_button = message
            ("xmlui.itemcloning.CloneItemTransfomer.version");
    private static final Message T_cancel_button = message("xmlui.general.cancel");

    /**
     * Initialize the page metadata & breadcrumb trail
     */
    @Override
    public void addPageMeta(PageMeta pageMeta) throws WingException {
        Item item = CloneItemUtils.getItem(context, objectModel);
        pageMeta.addMetadata("title").addContent(getTitle(item));
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrail().addContent(T_trail);
    }

    private Message getTitle(Item item) {
        Message title;
        if (item != null) {
            title = T_title.parameterize(item.getName());
        } else {
            title = T_title;
        }
        return title;
    }

    @Override
    public void addBody(Body body) throws WingException {
        Division division = body.addDivision("CloneItemTransfomer", "primary");
        Item item = CloneItemUtils.getItem(context, objectModel);
        if (item != null) {
            if (CloneItemUtils.authorizeCloning(objectModel)) {
                addMain(division, item);
            } else {
                bodyNotAuthorized(division);
            }
        } else {
            bodyNotFound(division);
        }
    }

    private void addMain(Division division, Item item) throws WingException {
        division.setHead(getTitle(item));
        Division main = division.addInteractiveDivision
                ("main", contextPath + "/clone-item", Division.METHOD_POST);
        List form = main.addList("form", List.TYPE_FORM);
        org.dspace.app.xmlui.wing.element.Item formItem = form.addItem();
        formItem.addHidden("itemID").setValue(String.valueOf(item.getID()));
        addButton(formItem, "onderdeel", T_onderdeel_button);
        addButton(formItem, "version", T_version_button);
        addButton(formItem, "cancel", T_cancel_button);
    }

    private void addButton(
            org.dspace.app.xmlui.wing.element.Item formItem,
            String name,
            Message label
    ) throws WingException {
        Button button = formItem.addButton(name);
        button.setValue(label);
    }

    private void bodyNotFound(Division division) throws WingException {
        division.setHead(getTitle(null));
        Para para = division.addPara();
        para.addContent(T_not_found);
    }

    private void bodyNotAuthorized(Division division) throws WingException {
        division.setHead(getTitle(null));
        Para para = division.addPara();
        para.addContent(T_not_authorized);
    }
}