package com.accordion.service;

import com.accordion.model.Channel;
import com.accordion.repository.ChannelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChannelServiceTest {

    @Mock
    private ChannelRepository channelRepository;

    @Mock
    private UsageReportingService usageReportingService;

    @InjectMocks
    private ChannelService channelService;

    private Channel testChannel;

    @BeforeEach
    void setUp() {
        testChannel = new Channel("general", "General discussion", "System");
    }

    @Test
    void testCreateChannel() {
        when(channelRepository.findByName("general")).thenReturn(Optional.empty());
        when(channelRepository.save(any(Channel.class))).thenReturn(testChannel);

        Channel result = channelService.createChannel("general", "General discussion", "System");

        assertNotNull(result);
        assertEquals("general", result.getName());
        assertEquals("General discussion", result.getDescription());
        assertEquals("System", result.getCreatedBy());
        verify(channelRepository).findByName("general");
        verify(channelRepository).save(any(Channel.class));
        verify(usageReportingService).reportChannelCreated();
    }

    @Test
    void testCreateChannelWithDuplicateName() {
        when(channelRepository.findByName("general")).thenReturn(Optional.of(testChannel));

        assertThrows(IllegalArgumentException.class, () ->
            channelService.createChannel("general", "General discussion", "System")
        );

        verify(channelRepository).findByName("general");
        verify(channelRepository, never()).save(any(Channel.class));
        verify(usageReportingService, never()).reportChannelCreated();
    }

    @Test
    void testGetAllChannels() {
        Channel channel1 = new Channel("general", "General discussion", "System");
        Channel channel2 = new Channel("random", "Random stuff", "Admin");
        List<Channel> channels = Arrays.asList(channel1, channel2);

        when(channelRepository.findAll()).thenReturn(channels);

        List<Channel> result = channelService.getAllChannels();

        assertEquals(2, result.size());
        assertEquals("general", result.get(0).getName());
        assertEquals("random", result.get(1).getName());
        verify(channelRepository).findAll();
    }

    @Test
    void testGetChannelById() {
        when(channelRepository.findById(1L)).thenReturn(Optional.of(testChannel));

        Optional<Channel> result = channelService.getChannelById(1L);

        assertTrue(result.isPresent());
        assertEquals("general", result.get().getName());
        verify(channelRepository).findById(1L);
    }

    @Test
    void testGetChannelByIdNotFound() {
        when(channelRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<Channel> result = channelService.getChannelById(999L);

        assertFalse(result.isPresent());
        verify(channelRepository).findById(999L);
    }

    @Test
    void testGetChannelByName() {
        when(channelRepository.findByName("general")).thenReturn(Optional.of(testChannel));

        Optional<Channel> result = channelService.getChannelByName("general");

        assertTrue(result.isPresent());
        assertEquals("general", result.get().getName());
        verify(channelRepository).findByName("general");
    }

    @Test
    void testGetOrCreateDefaultChannel() {
        when(channelRepository.findByName("general")).thenReturn(Optional.of(testChannel));

        Channel result = channelService.getOrCreateDefaultChannel();

        assertNotNull(result);
        assertEquals("general", result.getName());
        verify(channelRepository).findByName("general");
        verify(channelRepository, never()).save(any(Channel.class));
    }

    @Test
    void testGetOrCreateDefaultChannelCreatesNew() {
        when(channelRepository.findByName("general")).thenReturn(Optional.empty());
        when(channelRepository.save(any(Channel.class))).thenReturn(testChannel);

        Channel result = channelService.getOrCreateDefaultChannel();

        assertNotNull(result);
        assertEquals("general", result.getName());
        verify(channelRepository).findByName("general");
        verify(channelRepository).save(any(Channel.class));
    }
}
