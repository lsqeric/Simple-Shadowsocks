public class Config {
    private int localPort = 1089;
    private String remoteAddress = "13.230.233.193"; //
    private int remotePort = 33189;
    private String method = "aes-256-cfb";
    private String password = "Authorware0922";

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
