/**
 * Holds references to both data drivers and provides access to them.
 * ThreadServer uses this to choose the appropriate driver per operation.
 */
class DataExchanger {

    private DataDriver httpDriver;
    private DataDriver mongoDriver;

    public DataExchanger(String apiURL, String mongoURL) {
        httpDriver  = new HttpDataDriver(apiURL);
        mongoDriver = new MongoDataDriver(mongoURL);
    }

    public DataDriver getHttpDriver() {
        return httpDriver;
    }

    public DataDriver getMongoDriver() {
        return mongoDriver;
    }
}
