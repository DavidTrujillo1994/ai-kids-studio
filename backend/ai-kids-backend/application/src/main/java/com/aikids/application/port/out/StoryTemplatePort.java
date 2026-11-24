package com.aikids.application.port.out;

import com.aikids.domain.story.StoryTemplate;
import java.util.List;

public interface StoryTemplatePort {

    List<StoryTemplate> all();
}
