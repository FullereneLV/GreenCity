package greencity.service.impl;

import greencity.constant.ErrorMessage;
import greencity.dto.comment.AddCommentDto;
import greencity.dto.comment.CommentReturnDto;
import greencity.entity.Comment;
import greencity.entity.Place;
import greencity.entity.User;
import greencity.exception.BadRequestException;
import greencity.exception.NotFoundException;
import greencity.repository.PlaceCommentRepo;
import greencity.service.PhotoService;
import greencity.service.PlaceCommentService;
import greencity.service.PlaceService;
import greencity.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

/**
 * The class provides implementation of the {@code CommentService}.
 *
 * @author Marian Milian
 * @version 1.0
 */
@Slf4j
@Service
@AllArgsConstructor
public class PlaceCommentServiceImpl implements PlaceCommentService {
    private UserService userService;
    private PlaceCommentRepo placeCommentRepo;
    private PlaceService placeService;
    private PhotoService photoService;
    private ModelMapper modelMapper;

    /**
     * {@inheritDoc}
     *
     * @author Marian Milian
     */
    @Override
    public CommentReturnDto findById(Long id) {
        Comment comment = placeCommentRepo.findById(id).orElseThrow(() -> new NotFoundException(""));
        return modelMapper.map(comment, CommentReturnDto.class);
    }

    /**
     * {@inheritDoc}
     *
     * @author Marian Milian
     */
    @Override
    public CommentReturnDto save(Long placeId, AddCommentDto addCommentDto, String email) {
        Place place = placeService.findById(placeId);
        User user = userService.findByEmail(email).orElseThrow(() ->
            new NotFoundException(ErrorMessage.USER_NOT_FOUND_BY_EMAIL));
        Comment comment = modelMapper.map(addCommentDto, Comment.class);
        comment.setPlace(place);
        comment.setUser(user);
        if (comment.getEstimate() != null) {
            comment.getEstimate().setUser(user);
            comment.getEstimate().setPlace(place);
        }
        comment.getPhotos().forEach(photo -> {
            if (photoService.findByName(photo.getName()).isPresent()) {
                throw new BadRequestException(ErrorMessage.PHOTO_IS_PRESENT);
            }
            photo.setUser(user);
            photo.setComment(comment);
        });

        return modelMapper.map(placeCommentRepo.save(comment), CommentReturnDto.class);
    }

    /**
     * {@inheritDoc}
     *
     * @author Marian Milian
     */
    @Override
    public void deleteById(Long id) {
        placeCommentRepo.delete(placeCommentRepo.findById(id).orElseThrow(() -> new NotFoundException("")));
    }
}