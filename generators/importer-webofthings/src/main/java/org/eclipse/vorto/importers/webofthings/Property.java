package org.eclipse.vorto.importers.webofthings;

public class Property {

    private String name;

    private String description;

    private boolean readonly;

    private String type;

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setReadonly(boolean readonly) {
        this.readonly = readonly;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isReadonly() {
        return readonly;
    }

    public String getType() {
        return type;
    }
}
