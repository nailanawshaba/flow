package com.vaadin.client.communication.tree;

import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.vaadin.client.ChangeUtil;
import com.vaadin.shared.communication.MethodInvocation;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonType;

public class TestBasicTreeOperations extends AbstractTreeUpdaterTest {
    public void testAddRemoveElements() {
        int containerChildrenId = 6;
        int divId = 3;
        int spanId = 4;
        int imgId = 5;

        applyChanges(ChangeUtil.createList(containerChildrenId),
                ChangeUtil.putList(containerElementId, "CHILDREN",
                        containerChildrenId),
                ChangeUtil.createMap(divId),
                ChangeUtil.listInsertNode(containerChildrenId, 0, divId),
                ChangeUtil.put(divId, "TAG", "div"));

        Element root = updater.getRootElement();
        assertEquals(1, root.getChildCount());

        Element divChild = root.getFirstChildElement();
        assertEquals("DIV", divChild.getTagName());
        assertFalse(divChild.hasAttribute("tag"));

        applyChanges(ChangeUtil.createMap(spanId),
                ChangeUtil.listInsertNode(containerChildrenId, 1, spanId),
                ChangeUtil.put(spanId, "TAG", "span"));
        assertEquals(2, root.getChildCount());

        Element spanChild = divChild.getNextSiblingElement();
        assertEquals("SPAN", spanChild.getTagName());

        applyChanges(ChangeUtil.createMap(imgId),
                ChangeUtil.listInsertNode(containerChildrenId, 2, imgId),
                ChangeUtil.put(imgId, "TAG", "img"));
        assertEquals(divId, root.getChildCount());

        Element imgChild = Element.as(root.getChild(2));
        assertEquals("IMG", imgChild.getTagName());

        // Remove middle child
        applyChanges(ChangeUtil.listRemove(containerChildrenId, 1));
        assertEquals(2, root.getChildCount());
        assertNull(spanChild.getParentElement());

        // Remove first child even though it's no longer in the DOM
        spanChild.removeFromParent();
        applyChanges(ChangeUtil.listRemove(containerChildrenId, 0));
        assertEquals(1, root.getChildCount());
        assertEquals(root, imgChild.getParentElement());
    }

    public void testAttributesAndProperties() {
        Element element = updater.getRootElement();

        // Should only set property, not attribute
        applyChanges(ChangeUtil.put(containerElementId, "foo", "bar"));
        assertFalse(element.hasAttribute("foo"));
        assertEquals("bar", element.getPropertyString("foo"));

        applyChanges(ChangeUtil.remove(containerElementId, "foo"));
        assertFalse(element.hasAttribute("foo"));
        assertNull(element.getPropertyString("foo"));

        // Style should be set as attribute, not property
        applyChanges(
                ChangeUtil.put(containerElementId, "style", "height: 100%"));
        assertEquals("100%", element.getStyle().getHeight());

        applyChanges(ChangeUtil.remove(containerElementId, "style"));
        assertEquals("", element.getStyle().getHeight());

        // attr. prefixed attributes should be set as attributes only
        applyChanges(ChangeUtil.put(containerElementId, "attr.fox", "bax"));
        assertFalse(element.hasAttribute("attr.fox"));
        assertTrue(element.hasAttribute("fox"));
        assertEquals("bax", element.getAttribute("fox"));
        assertNull(element.getPropertyString("class"));

        applyChanges(ChangeUtil.remove(containerElementId, "attr.fox"));
        assertFalse(element.hasAttribute("fox"));
        assertEquals("", element.getAttribute("fox"));
    }

    public void testClassList() {
        Element element = updater.getRootElement();
        JsonArray array = Json.createArray();
        array.set(0, "foo");
        array.set(1, "bar");
        final int classListNodeId = 500;
        applyChanges(ChangeUtil.createList(classListNodeId),
                ChangeUtil.putList(containerElementId, "CLASS_LIST",
                        classListNodeId),
                ChangeUtil.listInsert(classListNodeId, 0, array));

        assertTrue(element.hasAttribute("class"));
        assertEquals("foo bar", element.getAttribute("class"));

        // add baz
        array = Json.createArray();
        array.set(0, "baz");
        applyChanges(
                ChangeUtil.putList(containerElementId, "CLASS_LIST",
                        classListNodeId),
                ChangeUtil.listInsert(classListNodeId, 2, array));

        assertTrue(element.hasAttribute("class"));
        assertEquals("foo bar baz", element.getAttribute("class"));

        // remove bar
        applyChanges(ChangeUtil.listRemove(classListNodeId, 1));

        assertTrue(element.hasAttribute("class"));
        assertEquals("foo baz", element.getAttribute("class"));

        // remove foo
        applyChanges(ChangeUtil.listRemove(classListNodeId, 0));

        assertTrue(element.hasAttribute("class"));
        assertEquals("baz", element.getAttribute("class"));

        // remove baz, class is not removed completely but will be an empty
        // string
        applyChanges(ChangeUtil.listRemove(classListNodeId, 0));

        assertTrue(element.hasAttribute("class"));
        assertEquals("", element.getAttribute("class"));
    }

    public void testEventHandling() {
        int eventDataId = 3;
        int listenersId = 4;
        int clickDataId = 5;

        applyChanges(ChangeUtil.createList(listenersId),
                ChangeUtil.putList(containerElementId, "LISTENERS",
                        listenersId),
                ChangeUtil.listInsert(listenersId, 0, "click"),
                ChangeUtil.createMap(eventDataId),
                ChangeUtil.putMap(containerElementId, "EVENT_DATA",
                        eventDataId),
                ChangeUtil.createList(clickDataId),
                ChangeUtil.putList(eventDataId, "click", clickDataId),
                ChangeUtil.listInsert(clickDataId, 0, "clientX"));

        NativeEvent event = Document.get().createClickEvent(0, 1, 2, 3, 4,
                false, false, false, false);
        updater.getRootElement().dispatchEvent(event);

        List<MethodInvocation> enqueuedInvocations = updater
                .getEnqueuedInvocations();
        assertEquals(1, enqueuedInvocations.size());

        MethodInvocation invocation = enqueuedInvocations.get(0);
        assertEquals("com.vaadin.ui.JavaScript$JavaScriptCallbackRpc",
                invocation.getInterfaceName());
        assertEquals("call", invocation.getMethodName());
        assertEquals("vEvent", invocation.getJavaScriptCallbackRpcName());

        JsonArray parameters = invocation.getParameters();
        assertEquals(3, parameters.length());

        // node ID
        assertEquals(containerElementId, (int) parameters.getNumber(0));
        // Event name
        assertEquals(event.getType(), parameters.getString(1));
        // Event data
        JsonObject eventData = parameters.getObject(2);
        assertEquals(1, eventData.keys().length);
        assertEquals(event.getClientX(), (int) eventData.getNumber("clientX"));

        enqueuedInvocations.clear();

        // Remove the listener
        applyChanges(ChangeUtil.listRemove(listenersId, 0));

        // Fire a new event
        event = Document.get().createClickEvent(0, 1, 2, 3, 4, false, false,
                false, false);
        updater.getRootElement().dispatchEvent(event);

        assertEquals(0, enqueuedInvocations.size());
    }

    public void testReceivingRpc() {
        JsonArray invocation = Json.createArray();

        invocation.set(0, "$0.foobarProperty=$1");
        JsonObject elementDescriptor = Json.createObject();
        elementDescriptor.put("node", containerElementId);
        // BasicElementTemplate.id = 0
        elementDescriptor.put("template", 0);
        invocation.set(1, elementDescriptor);
        invocation.set(2, "Hello");

        JsonArray invocations = Json.createArray();
        invocations.set(0, invocation);

        applyRpc(invocations);

        assertEquals("Hello",
                updater.getRootElement().getPropertyString("foobarProperty"));
    }

    public void testGWTWorkaroundNeeded() {
        assertEquals(
                "GWT has been fixed, buildChanges no longer needs to re-parse the json",
                JsonType.OBJECT, Json.create(true).getType());
        assertEquals(
                "GWT has been fixed, buildChanges no longer needs to re-parse the json",
                JsonType.OBJECT, Json.create(1).getType());
    }

    public void testStructuredProperties() {
        Element element = updater.getRootElement();

        JsonObject existingObject = Json.createObject();
        existingObject.put("foo", "bar");
        element.setPropertyJSO("existingobject",
                (JavaScriptObject) existingObject);

        JsonArray existingArray = Json.createArray();
        element.setPropertyJSO("existingarray",
                (JavaScriptObject) existingArray);

        int existingObjectId = 3;
        int newObjectId = 4;
        int existingMemberId = 5;
        int newMemberId = 6;
        int existingArrayId = 7;
        int newArrayId = 8;

        applyChanges(ChangeUtil.createMap(existingObjectId),
                ChangeUtil.putMap(containerElementId, "existingobject",
                        existingObjectId),
                ChangeUtil.put(existingObjectId, "baz", "asdf"),
                ChangeUtil.createMap(newObjectId),
                ChangeUtil.putMap(containerElementId, "newobject", newObjectId),
                ChangeUtil.put(newObjectId, "new", "value"),
                ChangeUtil.createList(existingArrayId),
                ChangeUtil.putList(containerElementId, "existingarray",
                        existingArrayId),
                ChangeUtil.createMap(existingMemberId),
                ChangeUtil.listInsertNode(existingArrayId, 0, existingMemberId),
                ChangeUtil.put(existingMemberId, "value", "yes"),
                ChangeUtil.createList(newArrayId),
                ChangeUtil.putList(containerElementId, "newarray", newArrayId),
                ChangeUtil.createMap(newMemberId),
                ChangeUtil.listInsertNode(newArrayId, 0, newMemberId),
                ChangeUtil.put(newMemberId, "checked", Json.create(true)));

        assertSame(existingObject, element.getPropertyJSO("existingobject"));
        assertEquals("bar", existingObject.getString("foo"));
        assertEquals("asdf", existingObject.getString("baz"));

        JsonObject newObject = element.getPropertyJSO("newobject").cast();
        assertEquals("value", newObject.getString("new"));

        assertSame(existingArray, element.getPropertyJSO("existingarray"));
        assertEquals(1, existingArray.length());
        assertEquals("yes", existingArray.getObject(0).getString("value"));

        JsonArray newArray = element.getPropertyJSO("newarray").cast();
        assertEquals(1, newArray.length());
        assertTrue(newArray.getObject(0).getBoolean("checked"));
    }

    public void testStructuredProperties_basicArray() {
        int arrayId = 3;
        applyChanges(ChangeUtil.createList(arrayId),
                ChangeUtil.putList(containerElementId, "array", arrayId),
                ChangeUtil.listInsert(arrayId, 0, "Hello"));

        Element element = updater.getRootElement();
        JsonArray array = element.getPropertyJSO("array").cast();

        assertEquals(1, array.length());
        assertEquals("Hello", array.getString(0));
    }

    public void testEventDataCallback() {
        int eventDataId = 3;
        int clickDataId = 5;
        int listenersId = 4;

        applyChanges(ChangeUtil.createList(listenersId),
                ChangeUtil.putList(containerElementId, "LISTENERS",
                        listenersId),
                ChangeUtil.listInsert(listenersId, 0, "click"),
                ChangeUtil.createMap(eventDataId),
                ChangeUtil.putMap(containerElementId, "EVENT_DATA",
                        eventDataId),
                ChangeUtil.createList(clickDataId),
                ChangeUtil.putList(eventDataId, "click", clickDataId),
                ChangeUtil.listInsert(clickDataId, 0,
                        "[typeof event, element.tagName]"));

        NativeEvent event = Document.get().createClickEvent(0, 1, 2, 3, 4,
                false, false, false, false);
        updater.getRootElement().dispatchEvent(event);

        List<MethodInvocation> enqueuedInvocations = updater
                .getEnqueuedInvocations();
        assertEquals(1, enqueuedInvocations.size());

        MethodInvocation invocation = enqueuedInvocations.get(0);
        JsonArray parameters = invocation.getParameters();

        JsonObject eventData = parameters.getObject(2);
        assertEquals(1, eventData.keys().length);

        JsonArray jsonValue = eventData.get("[typeof event, element.tagName]");
        assertEquals(JsonType.ARRAY, jsonValue.getType());
        assertEquals(2, jsonValue.length());
        assertEquals("object", jsonValue.getString(0));
        assertEquals("DIV", jsonValue.getString(1));
    }

    public void testReceiveValueTypes() {
        JsonObject todoPropertiesJson = Json.createObject();
        todoPropertiesJson.put("completed", 2);
        todoPropertiesJson.put("title", 0);
        todoPropertiesJson.put("editing", 2);

        JsonObject todoTypeJson = Json.createObject();
        todoTypeJson.put("properties", todoPropertiesJson);

        JsonObject todosTypeJson = Json.createObject();
        todosTypeJson.put("member", 10);

        JsonObject modelPropertiesJson = Json.createObject();
        modelPropertiesJson.put("todos", 11);
        modelPropertiesJson.put("todoCount", 4);
        modelPropertiesJson.put("completeCount", 4);
        modelPropertiesJson.put("remainingCount", 4);

        JsonObject modelTypeJson = Json.createObject();
        modelTypeJson.put("properties", modelPropertiesJson);

        JsonObject valueTypes = Json.createObject();
        valueTypes.put("10", todoTypeJson);
        valueTypes.put("11", todosTypeJson);
        valueTypes.put("12", modelTypeJson);

        updater.update(valueTypes, Json.createObject(), Json.createArray(),
                Json.createArray());

        ValueTypeMap typeMap = updater.getTypeMap();
        ValueType primitiveInt = typeMap.get(ValueTypeMap.INTEGER_PRIMITIVE);
        ValueType stringType = typeMap.get(ValueTypeMap.STRING);
        ValueType primitiveBoolean = typeMap
                .get(ValueTypeMap.BOOLEAN_PRIMITIVE);

        ValueType modelType = typeMap.get(12);

        assertNull(modelType.getDefaultValue());
        assertNull(modelType.getMemberType());

        Map<String, ValueType> modelProperties = modelType.getProperties();

        assertEquals(4, modelProperties.size());

        assertSame(primitiveInt, modelProperties.get("todoCount"));
        assertSame(primitiveInt, modelProperties.get("completeCount"));
        assertSame(primitiveInt, modelProperties.get("remainingCount"));

        ValueType todosType = modelProperties.get("todos");
        assertSame(todosType, typeMap.get(11));
        assertNull(todosType.getDefaultValue());
        assertNull(todosType.getProperties());

        ValueType todoType = todosType.getMemberType();
        assertSame(todoType, typeMap.get(10));
        assertNull(todoType.getDefaultValue());
        assertNull(todoType.getMemberType());

        Map<String, ValueType> todoProperties = todoType.getProperties();

        assertEquals(3, todoProperties.size());
        assertSame(primitiveBoolean, todoProperties.get("completed"));
        assertSame(primitiveBoolean, todoProperties.get("editing"));
        assertSame(stringType, todoProperties.get("title"));

    }

    private static native String typeOf(
            Object object) /*-{ return typeof object; }-*/;
}