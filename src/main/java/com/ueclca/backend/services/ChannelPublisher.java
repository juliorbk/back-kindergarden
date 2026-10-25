package com.ueclca.backend.services;

import com.ueclca.backend.models.Channel;
import com.ueclca.backend.models.Post;

public interface ChannelPublisher {

    Channel channel();

    String publish(Post post);
}
