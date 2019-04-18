package amazmod.com.transport.data;

public class FileData {

    private String path;
    private String name;
    private boolean isDirectory;
    private long size;
    private long creationDate;
    private long lastEditDate;
    private long permissions;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public String getExtention() {
        return ( name.lastIndexOf(".")>=0 )? name.substring(name.lastIndexOf(".")).replace(".","") : "";
    }

    public void setDirectory(boolean directory) {
        isDirectory = directory;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(long creationDate) {
        this.creationDate = creationDate;
    }

    public long getLastEditDate() {
        return lastEditDate;
    }

    public void setLastEditDate(long lastEditDate) {
        this.lastEditDate = lastEditDate;
    }

    public long getPermissions() {
        return permissions;
    }

    public void setPermissions(long permissions) {
        this.permissions = permissions;
    }
}
