package com.example.demo.domain.group;

import com.example.demo.domain.appUser.User;
import com.example.demo.domain.appUser.UserRepository;
import com.example.demo.domain.role.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class GroupServiceImpl implements GroupService {

    @Autowired
    private final GroupRepository groupRepository;
    @Autowired
    private final UserRepository userRepository;

    public GroupServiceImpl(GroupRepository groupRepository, UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
    }

    /**
     * Finds all groups in the database it does not need any params
     * because it gets all groups regardless of their id
     * @return
     */
    @Override
    public List<Group> findAll() {
        return groupRepository.findAll();
    }

    /**
     * Deletes a group by its id if it does not find the id
     * it will throw an InstanceNotFoundException
     *
     * @param uuid -> For finding group
     * @return -> Message if the group got deleted
     */
    @Override
    public String delete(UUID uuid) {
        if (groupRepository.existsById(uuid)){
            groupRepository.delete(findById(uuid));
            return "Group deleted";
        } else {
            return "Group not found";
        }
    }

    /**
     * Posts a group in Database if the group already exists, which
     * is checked by the username, it throws an InstanceAlreadyExistsException
     *
     * @param group -> Body of Group (Data which is going to get inserted)
     * @return -> The data that got inserted
     * @throws InstanceAlreadyExistsException -> Gets thrown if the group
     * already exists
     */
    @Override
    public Group saveGroup(Group group) throws InstanceAlreadyExistsException {
        if (groupRepository.findByName(group.getName()) != null) {
            return null;
        } else {
            return groupRepository.save(group);
        }
    }

    /**
     * Finds Group by its name
     *
     * @param groupName -> Is needed to find the group
     * @return -> The data that got found
     */
    @Override
    public Group getGroup(String groupName) {
        return groupRepository.findByName(groupName);
    }

    /**
     * Finds group by its id and print the users out by using pagination
     *
     * @param uuid -> To find the group
     * @param offset -> Which site you are on
     * @param pageSize -> How much should be shown on one site
     * @return -> All users in the group
     */
    @Override
    public Page<User> getAllUsersOfGroup(UUID uuid, int offset, int pageSize) {
        Pageable paging = PageRequest.of(offset, pageSize);
        Page<User> userPage = this.userRepository.findAllUserByGroupId(uuid, paging);
        userRepository.findAll();
        return userPage;
    }

    /**
     * Adds an user to a group and checks if he is already in another group
     *
     * @param username -> To find the user by his username
     * @param groupName -> To find the group by its groupname
     */
    @Override
    public String addUserToGroup(String username, String groupName) {
        User user = userRepository.findByUsername(username);
        Group group = groupRepository.findByName(groupName);
        if (user.getGroup() == null) {
            group.getUsers().add(user);
            groupRepository.save(group);
            return "User got added";
        } else {
            System.out.println("Already in group");
            return "Already in group";
        }
    }

    /**
     * Finds group by its id and returns it
     *
     * @param id -> To find the group by its id
     * @return -> Group or nothing
     */
    @Override
    public Group findById(UUID id) {
        Optional<Group> group = groupRepository.findById(id);
        return group.orElse(null);
    }

    /**
     * Finds group by its id and updates the data
     *
     * @param id -> To find the group
     * @param group -> The body with data which is going to get inserted
     * @return -> Body of data that got inserted
     */
    @Override
    public String updateGroup(UUID id, Group group) {
        groupRepository.findById(id)
                .map(group1 -> {
                    groupRepository.deleteById(id);
                    group1.setName(group.getName());
                    group1.setDescription(group.getDescription());
                    group1.setUsers(group.getUsers());
                    groupRepository.save(group1);
                    return "Group got updated";
                }).orElseGet(() -> {
                    group.setId(id);
                    groupRepository.save(group);
                    return "Group got inserted";
                });
        return "Group got updated";
    }

    /**
     * Gets the logged-in user and checks if he is an Admin or
     * a member of the group
     * @param uuid -> To find and compare the group
     * @return -> Boolean if he is authorized
     */
    @Override
    public boolean isUserAuthorizedForGroup(UUID uuid) {
        Object auth = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User currentUser = userRepository.findByUsername(((UserDetails)auth).getUsername());

        for (Role role : currentUser.getRoles()) {
            if (role.getName().equals("ADMIN")){
                return true;
            }
        }

        if (currentUser.getGroup().getId().equals(uuid)){
            return true;
        }else {
            return false;
        }
    }
}