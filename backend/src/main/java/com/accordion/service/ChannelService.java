package com.accordion.service;

import com.accordion.model.Channel;
import com.accordion.repository.ChannelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ChannelService {

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private UsageReportingService usageReportingService;

    public Channel createChannel(String name, String description, String createdBy) {
        // Check if channel with this name already exists
        if (channelRepository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("Channel with name '" + name + "' already exists");
        }
        
        Channel channel = new Channel(name, description, createdBy);
        Channel saved = channelRepository.save(channel);
        usageReportingService.reportChannelCreated();
        return saved;
    }

    public List<Channel> getAllChannels() {
        return channelRepository.findAll();
    }

    public Optional<Channel> getChannelById(Long id) {
        return channelRepository.findById(id);
    }

    public Optional<Channel> getChannelByName(String name) {
        return channelRepository.findByName(name);
    }

    public Channel getOrCreateDefaultChannel() {
        return channelRepository.findByName("general")
                .orElseGet(() -> {
                    Channel defaultChannel = new Channel("general", "General discussion", "System");
                    return channelRepository.save(defaultChannel);
                });
    }
}
