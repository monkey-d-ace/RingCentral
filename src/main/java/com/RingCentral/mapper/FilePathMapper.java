package com.RingCentral.mapper;

import com.RingCentral.model.FilePath;
import org.apache.ibatis.annotations.Mapper;
import tk.mybatis.mapper.common.BaseMapper;

@Mapper
public interface FilePathMapper extends BaseMapper<FilePath> {
}
