package utescore.service;

import org.springframework.stereotype.Service;
import utescore.entity.Poster;
import utescore.repository.PosterRepository;

import java.util.List;
import java.util.Optional;

@Service
public class PosterService {

    private final PosterRepository repository;

    public PosterService(PosterRepository repository) {
        this.repository = repository;
    }

    public List<Poster> getAll() {
        return repository.findAll();
    }

    public Optional<Poster> getById(Integer id) {
        return repository.findById(id);
    }

    public Poster save(Poster poster) {
        return repository.save(poster);
    }

    public void delete(Integer id) {
        repository.deleteById(id);
    }
}