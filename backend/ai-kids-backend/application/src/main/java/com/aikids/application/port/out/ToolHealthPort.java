package com.aikids.application.port.out;

import com.aikids.application.shared.ToolStatus;
import java.util.List;

public interface ToolHealthPort {

    List<ToolStatus> check();
}
