package org.zenframework.z8.server.base.table.value;

import org.zenframework.z8.server.base.json.Json;
import org.zenframework.z8.server.base.json.parser.JsonArray;
import org.zenframework.z8.server.base.json.parser.JsonObject;
import org.zenframework.z8.server.runtime.IObject;
import org.zenframework.z8.server.types.primary;

public class JsonField extends TextField {

    public static class CLASS<T extends JsonField> extends TextField.CLASS<T> {
        public CLASS(IObject container) {
            super(container);
            setJavaClass(JsonField.class);
            setAttribute("native", JsonField.class.getCanonicalName());
        }

        @Override
        public Object newObject(IObject container) {
            return new JsonField(container);
        }
    }

    public JsonField(IObject container) {
        super(container);
    }

    public org.zenframework.z8.server.json.Json getJson() {
        return new org.zenframework.z8.server.json.Json(get().toString());
    }

    public org.zenframework.z8.server.json.parser.JsonObject getJsonObject() {
        return new org.zenframework.z8.server.json.parser.JsonObject(get().toString());
    }

    public org.zenframework.z8.server.json.parser.JsonArray getJsonArray() {
        return new org.zenframework.z8.server.json.parser.JsonArray(get().toString());
    }

    public void set(Object object) {
        set(org.zenframework.z8.server.json.parser.JsonObject.wrap(object).toString());
    }

    public Json.CLASS<Json> z8_getJson() {
        Json.CLASS<Json> json = new Json.CLASS<Json>(null);
        json.get().set(getJson());
        return json;
    }
    
    public JsonObject.CLASS<? extends JsonObject> z8_getJsonObject() {
        JsonObject.CLASS<JsonObject> json = new JsonObject.CLASS<JsonObject>(null);
        json.get().set(getJsonObject());
        return json;
    }
    
    public JsonArray.CLASS<? extends JsonArray> z8_getJsonArray() {
        JsonArray.CLASS<JsonArray> json = new JsonArray.CLASS<JsonArray>(null);
        json.get().set(getJsonArray());
        return json;
    }
    
    @SuppressWarnings("unchecked")
    public TextField.CLASS<? extends TextField> operatorAssign(primary value) {
        set(org.zenframework.z8.server.json.parser.JsonObject.valueToString(value));
        return (TextField.CLASS<? extends TextField>) this.getCLASS();
    }

    @SuppressWarnings("unchecked")
    public TextField.CLASS<? extends TextField> operatorAssign(JsonObject.CLASS<? extends JsonObject> value) {
        set(value.get().getInternalObject().toString());
        return (TextField.CLASS<? extends TextField>) this.getCLASS();
    }

    @SuppressWarnings("unchecked")
    public TextField.CLASS<? extends TextField> operatorAssign(JsonArray.CLASS<? extends JsonArray> value) {
        set(value.get().getInternalArray().toString());
        return (TextField.CLASS<? extends TextField>) this.getCLASS();
    }

    @SuppressWarnings("unchecked")
    public TextField.CLASS<? extends TextField> operatorAssign(Json.CLASS<? extends Json> value) {
        set(value.get().getInternalJson());
        return (TextField.CLASS<? extends TextField>) this.getCLASS();
    }

}
