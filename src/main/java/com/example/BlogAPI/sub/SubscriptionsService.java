package com.example.BlogAPI.sub;

import com.example.BlogAPI.user.User;
import com.example.BlogAPI.user.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional(readOnly = true)
public class SubscriptionsService {

    private final UsersRepository usersRepository;

    @Autowired
    public SubscriptionsService(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    @Transactional
    public void followUser(Long followerId, Long followingId) {
        User follower = usersRepository.findById(followerId)
                .orElseThrow(() -> new RuntimeException("Follower not found"));

        User following = usersRepository.findById(followingId)
                .orElseThrow(() -> new RuntimeException("User to follow not found"));

        if (followerId.equals(followingId)) {
            throw new RuntimeException("Cannot follow yourself!");
        }

        if (follower.isFollowing(following)) {
            throw new RuntimeException("You already following this user");
        }

        follower.follow(following);
        usersRepository.save(follower);
    }

    @Transactional
    public void unfollowUser(Long followerId, Long followingId) {
        User follower = usersRepository.findById(followerId)
                .orElseThrow(() -> new RuntimeException("Follower not found"));

        User following = usersRepository.findById(followingId)
                .orElseThrow(() -> new RuntimeException("User to unfollow not found"));

        if (!follower.isFollowing(following)) {
            throw new RuntimeException("Not following this user");
        }

        follower.unfollow(following);
        usersRepository.save(follower);
    }

}
