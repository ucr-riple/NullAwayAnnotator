package edu.ucr.cs.riple.diagnose.explorer;

import org.json.simple.JSONObject;

import java.io.FileWriter;
import java.io.IOException;

public class ExplorerConfig {
    boolean SUGGEST;
    boolean LOG_ERROR;
    boolean LOG_ERROR_DEEP;
    boolean MAKE_METHOD_INHERITANCE_TREE;
    boolean METHOD_PARAM_TEST_ACTIVE;
    boolean OPTIMIZED;
    String ANNOTATION_NULLABLE;
    String ANNOTATION_NONNULL;
    Long METHOD_PARAM_TEST_INDEX;

    public ExplorerConfig() {
        SUGGEST = LOG_ERROR = LOG_ERROR_DEEP = MAKE_METHOD_INHERITANCE_TREE = METHOD_PARAM_TEST_ACTIVE = OPTIMIZED = false;
        ANNOTATION_NULLABLE = "javax.annotation.Nullable";
        ANNOTATION_NONNULL = "javax.annotation.Nonnull";
        METHOD_PARAM_TEST_INDEX = (long) 0;
    }

    @SuppressWarnings("unchecked")
    public void writeInJson(String path) {
        JSONObject res = new JSONObject();
        res.put("SUGGEST", SUGGEST);
        res.put("MAKE_METHOD_INHERITANCE_TREE", MAKE_METHOD_INHERITANCE_TREE);
        res.put("OPTIMIZED", OPTIMIZED);
        JSONObject annotation = new JSONObject();
        annotation.put("NULLABLE", ANNOTATION_NULLABLE);
        annotation.put("NONNULL", ANNOTATION_NONNULL);
        res.put("ANNOTATION", annotation);
        JSONObject logError = new JSONObject();
        logError.put("ACTIVE", LOG_ERROR);
        logError.put("DEEP", LOG_ERROR_DEEP);
        res.put("LOG_ERROR", logError);
        JSONObject paramTest = new JSONObject();
        paramTest.put("ACTIVE", METHOD_PARAM_TEST_ACTIVE);
        paramTest.put("INDEX", METHOD_PARAM_TEST_INDEX);
        res.put("METHOD_PARAM_TEST", paramTest);
        System.out.println("res: " + res.toJSONString());
        try {
            FileWriter file = new FileWriter(path);
            System.out.println("CAME HERE");
            file.write(res.toJSONString());
            file.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class ExploreConfigBuilder{
        private final ExplorerConfig config;

        public ExploreConfigBuilder() {
            this.config = new ExplorerConfig();
        }

        public ExploreConfigBuilder setSuggest(boolean value){
            config.SUGGEST = value;
            return this;
        }

        public ExploreConfigBuilder setSuggest(boolean suggest, String nullable, String nonnull){
            config.SUGGEST = suggest;
            if(!suggest){
                throw new RuntimeException("SUGGEST must be activated");
            }
            config.ANNOTATION_NULLABLE = nullable;
            config.ANNOTATION_NONNULL = nonnull;
            return this;
        }

        public ExploreConfigBuilder setLogError(boolean value, boolean isDeep){
            config.LOG_ERROR = value;
            if((!value) && isDeep) {
                throw new RuntimeException("Log error must be enabled to activate deep log error");
            }
            config.LOG_ERROR_DEEP = isDeep;
            return this;
        }

        public ExploreConfigBuilder setMethodInheritanceTree(boolean value){
            config.MAKE_METHOD_INHERITANCE_TREE = value;
            return this;
        }

        public ExploreConfigBuilder setMethodParamTest(boolean value, long index){
            config.METHOD_PARAM_TEST_ACTIVE = value;
            if(value && index < 0){
                throw new RuntimeException("Index cannot be less than zero");
            }
            config.METHOD_PARAM_TEST_INDEX = index;
            return this;
        }

        public ExploreConfigBuilder setOptimized(boolean value){
            config.OPTIMIZED = value;
            return this;
        }

        public ExplorerConfig build(){
            return config;
        }
    }
}
