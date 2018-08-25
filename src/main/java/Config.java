public class Config {
    private int localPort = 1089; 
    private String remoteAddress = "Your Remote Server Address"; 
    private int remotePort = 443; // Replace with your remote server's port
    private String method = "aes-256-cfb";
    private String password = "xxxxxxxxxx"; // Replace with your remote server's password

    private static volatile Config config;

    public static Config getInstance(){
        if(config==null){
            synchronized (Config.class){
                if(config==null){
                    config = new Config();
                }
            }
        }
        return config;
    }

    public int getLocalPort() {
        return localPort;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public String getMethod() {
        return method;
    }

    public String getPassword(){
        return password;
    }


}
