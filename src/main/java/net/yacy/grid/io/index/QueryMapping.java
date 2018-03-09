package net.yacy.grid.io.index;

public enum QueryMapping implements MappingDeclaration {
    
    query_t,origin_s,
    collection_sxt,
    timezoneOffset_i,
    startRecord_i,
    maximumRecords_i,
    date_dt,hits_i,
    compiled_s,
    count_i,
    title_txt,
    url_sxt,
    snippet_txt,
    last_modified_dts,
    size_val;

    @Override
    public Mapping getMapping() {
        // TODO Auto-generated method stub
        return null;
    }
  
}
